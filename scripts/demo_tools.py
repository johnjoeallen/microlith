#!/usr/bin/env python3
"""Tools behind scripts/smoke.sh and scripts/trace.sh (Python 3 standard library only).

  demo_tools.py trace <correlationId>    merged call tree from every running demo process
  demo_tools.py smoke                    the scenarios of section 16.5 against the process that hosts orders
"""
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

DEMO = Path(__file__).resolve().parent.parent / ".demo"

GROUPS = {"front": {"orders", "notifications"},
          "fulfilment": {"catalog", "pricing", "inventory", "shipping"},
          "accounts": {"customers", "payments"}}


def http(method, url, body=None, headers=None, timeout=60):
    request = urllib.request.Request(url, data=body.encode() if body is not None else None, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()


def processes():
    path = DEMO / "processes"
    if not path.exists():
        sys.exit("no demo is running (scripts/demo.sh <mode> up)")
    return [(n, int(p)) for n, p, _ in (line.split() for line in path.read_text().splitlines() if line.strip())]


def collect(correlation_id):
    records = []
    for name, port in processes():
        status, text = http("GET", f"http://127.0.0.1:{port}/demo/trace?correlationId={correlation_id}", timeout=10)
        if status == 200:
            for record in json.loads(text):
                record["process"] = name
                records.append(record)
    return sorted(records, key=lambda r: (r["startedAtMicros"], r["depth"]))


def edge_line(record):
    return f"{record['caller']} -> {record['callee']}.{record['method']} [{record['binding']}] {round(record['durationMs'])} ms {record['outcome']}"


def tree(records):
    return "\n".join("  " * r["depth"] + "  " + edge_line(r) for r in records)


def expected_binding(mode, caller, callee):
    if mode in ("modulith", "macro-monolith"):
        return "local"
    if mode in ("loopback-codec", "loopback-http"):
        return mode
    if mode in ("groups", "split-dbs"):
        same = any(caller in g and callee in g for g in GROUPS.values())
        return "local" if same else "remote"
    return "remote"  # microservices, microservices-macro


# Calls (caller, callee, method, depth) of one happy-path order with one line: the same in every mode.
HAPPY_SHAPE = sorted([
    ("orders", "customers", "getCustomer", 0), ("orders", "pricing", "quote", 0), ("pricing", "customers", "getCustomer", 1),
    ("pricing", "catalog", "getProduct", 1), ("orders", "shipping", "quote", 0), ("shipping", "customers", "getCustomer", 1),
    ("shipping", "inventory", "getStock", 1), ("inventory", "catalog", "productExists", 2), ("orders", "inventory", "reserve", 0),
    ("orders", "payments", "authorize", 0), ("payments", "customers", "getPaymentMethod", 1), ("orders", "shipping", "createShipment", 0),
    ("shipping", "customers", "getCustomer", 1), ("shipping", "inventory", "getReservation", 1), ("orders", "payments", "capture", 0),
    ("orders", "notifications", "send", 0), ("notifications", "customers", "getCustomer", 1)])


class Smoke:
    def __init__(self):
        self.mode = (DEMO / "mode").read_text().strip()
        self.orders_url = (DEMO / "orders.url").read_text().strip()
        self.failures = []
        self.cids = []
        self.happy_cid = None

    def place(self, request_id, customer, sku, quantity, cid):
        self.cids.append(cid)
        body = {"jsonrpc": "2.0", "method": "orders.placeOrder", "id": 1,
                "params": {"requestId": request_id, "customerId": customer, "lines": [{"sku": sku, "quantity": quantity}]}}
        status, text = http("POST", self.orders_url + "/rpc/orders", json.dumps(body),
                            {"Content-Type": "application/json", "X-Correlation-Id": cid})
        if status != 200:
            raise AssertionError(f"orders answered HTTP {status}: {text[:200]}")
        return json.loads(text)

    def wait_for(self, cid, predicate, what, timeout=45):
        deadline = time.time() + timeout
        while True:
            records = collect(cid)
            if predicate(records):
                return records
            if time.time() > deadline:
                raise AssertionError(f"timed out waiting for {what}; trace so far:\n{tree(records)}")
            time.sleep(0.5)

    @staticmethod
    def expect(condition, message):
        if not condition:
            raise AssertionError(message)

    def scenario(self, name, function):
        try:
            function()
            print(f"  PASS  {name}")
        except AssertionError as error:
            print(f"  FAIL  {name}: {error}")
            self.failures.append(name)
        except Exception as error:  # noqa: BLE001 - report any unexpected failure as a scenario failure
            print(f"  FAIL  {name}: {type(error).__name__}: {error}")
            self.failures.append(name)

    @staticmethod
    def calls(records, callee=None, method=None):
        return [r for r in records if (callee is None or r["callee"] == callee) and (method is None or r["method"] == method)]

    def new(self, label):
        return f"smoke-{label}-{uuid.uuid4().hex[:8]}"

    # ---- scenarios ---------------------------------------------------------------------------------------------
    def happy_path(self):
        cid = self.happy_cid = self.new("happy")
        response = self.place(self.new("req"), "cust-standard", "sku-widget", 1, cid)
        order = response["result"]
        self.expect(order["status"] == "CONFIRMED", f"status {order['status']}")
        self.expect((order["subtotalCents"], order["discountCents"], order["totalCents"]) == (1000, 0, 1600), f"amounts {order}")
        records = self.wait_for(cid, lambda rs: len(self.calls(rs, "notifications", "send")) > 0 and len(self.calls(rs, "customers")) >= 5
                                and any(r["caller"] == "notifications" for r in rs), "the notification to be delivered")
        shape = sorted((r["caller"], r["callee"], r["method"], r["depth"]) for r in records)
        self.expect(shape == HAPPY_SHAPE, f"call shape differs.\n  expected {HAPPY_SHAPE}\n  actual   {shape}")
        self.expect(all(r["outcome"] == "ok" for r in records), "a call failed: " + ", ".join(edge_line(r) for r in records if r["outcome"] != "ok"))

    def gold_discount(self):
        cid = self.new("gold")
        order = self.place(self.new("req"), "cust-gold", "sku-widget", 2, cid)["result"]
        self.expect(order["status"] == "CONFIRMED", f"status {order['status']}")
        self.expect((order["subtotalCents"], order["discountCents"], order["totalCents"]) == (2000, 200, 2500), f"amounts {order}")
        records = collect(cid)
        self.expect(any(r["caller"] == "pricing" and r["callee"] == "customers" for r in records), "pricing did not ask customers for the tier")

    def out_of_stock(self):
        cid = self.new("oos")
        response = self.place(self.new("req"), "cust-standard", "sku-oos", 1, cid)
        error = response.get("error")
        self.expect(error is not None and error["code"] == 1001, f"expected error 1001, got {response}")
        self.expect(error["data"]["exception"] == "com.acme.inventory.api.OutOfStockException", f"exception {error['data']}")
        records = collect(cid)
        self.expect(any(r["callee"] == "inventory" and r["method"] == "reserve" and r["outcome"] == "error:OutOfStockException" for r in records),
                    "reserve did not fail with OutOfStockException")
        self.expect(not self.calls(records, "payments"), "payments was called although nothing was reserved")

    def payment_declined(self):
        cid = self.new("declined")
        order = self.place(self.new("req"), "cust-declined", "sku-widget", 1, cid)["result"]
        self.expect(order["status"] == "REJECTED", f"status {order['status']}")
        records = self.wait_for(cid, lambda rs: len(self.calls(rs, "inventory", "release")) > 0, "inventory.release")
        reserve = self.calls(records, "inventory", "reserve")
        release = self.calls(records, "inventory", "release")
        self.expect(reserve and release and reserve[0]["startedAtMicros"] < release[0]["startedAtMicros"], "reserve must come before release")
        self.expect(not self.calls(records, "shipping", "createShipment"), "a shipment was created for a declined order")

    def shipping_failure(self):
        cid = self.new("undeliverable")
        order = self.place(self.new("req"), "cust-standard", "sku-undeliverable", 1, cid)["result"]
        self.expect(order["status"] == "FAILED", f"status {order['status']}")
        records = self.wait_for(cid, lambda rs: self.calls(rs, "payments", "voidAuthorization") and self.calls(rs, "inventory", "release"),
                                "payments.voidAuthorization and inventory.release")
        self.expect(any(r["method"] == "createShipment" and r["outcome"] == "error:UndeliverableException" for r in records), "createShipment did not fail")
        self.expect(not self.calls(records, "payments", "capture"), "payment was captured for a failed order")

    def dependency_down(self):
        cid = self.new("notify")
        self.set_faults({"notificationsDown": True})
        try:
            order = self.place(self.new("req"), "cust-standard", "sku-widget", 1, cid)["result"]
            self.expect(order["status"] == "CONFIRMED", f"order failed while notifications was down: {order['status']}")
            self.wait_for(cid, lambda rs: any(r["method"] == "send" and r["outcome"].startswith("error:") for r in rs), "a failed delivery attempt")
        finally:
            self.set_faults({"notificationsDown": False})
        self.wait_for(cid, lambda rs: any(r["method"] == "send" and r["outcome"] == "ok" for r in rs), "delivery after the fault was cleared")

    def set_faults(self, faults):
        applied = 0
        for _, port in processes():
            status, text = http("POST", f"http://127.0.0.1:{port}/demo/faults", json.dumps(faults), {"Content-Type": "application/json"})
            self.expect(status == 200, f"/demo/faults answered {status}")
            applied += len(json.loads(text)["applied"])
        self.expect(applied > 0, "no process applied the fault")

    def idempotent_retry(self):
        request_id = self.new("req")
        first_cid, second_cid = self.new("retry1"), self.new("retry2")
        first = self.place(request_id, "cust-standard", "sku-gadget", 1, first_cid)["result"]
        second = self.place(request_id, "cust-standard", "sku-gadget", 1, second_cid)["result"]
        self.expect(first["orderId"] == second["orderId"], "a second order was created")
        self.expect(first == second, "the repeated request returned something different")
        repeated = collect(second_cid)
        self.expect(not repeated, "the repeated request called other modules again:\n" + tree(repeated))
        self.expect(len(self.calls(collect(first_cid), "inventory", "reserve")) == 1, "more than one reservation")

    # ---- cross-cutting -----------------------------------------------------------------------------------------
    def check_bindings(self):
        seen = set()
        problems = []
        for cid in self.cids:
            for r in collect(cid):
                seen.add(r["binding"])
                want = expected_binding(self.mode, r["caller"], r["callee"])
                if r["binding"] != want:
                    problems.append(f"{r['caller']} -> {r['callee']}.{r['method']} used [{r['binding']}], expected [{want}]")
        (DEMO / "last-bindings").write_text(",".join(sorted(seen)))
        if problems:
            raise AssertionError("; ".join(sorted(set(problems))))
        return seen

    def run(self):
        print(f"smoke test, mode '{self.mode}', orders at {self.orders_url}")
        for name, function in (("happy path, standard customer", self.happy_path), ("tier discount through the chain", self.gold_discount),
                               ("out of stock", self.out_of_stock), ("payment declined with compensation", self.payment_declined),
                               ("shipping failure with two compensations", self.shipping_failure),
                               ("dependency down (notifications), delivered later", self.dependency_down),
                               ("idempotent retry", self.idempotent_retry)):
            self.scenario(name, function)
        self.scenario(f"every edge uses the binding expected for '{self.mode}'", self.check_bindings)
        if self.happy_cid:
            print(f"\nmerged call tree of one order (correlation id {self.happy_cid}):\n{tree(collect(self.happy_cid))}")
        print(f"\nbindings seen: {(DEMO / 'last-bindings').read_text() if (DEMO / 'last-bindings').exists() else '-'}")
        if self.failures:
            print(f"FAILED: {len(self.failures)} scenario(s): {', '.join(self.failures)}")
            return 1
        print("all scenarios passed")
        return 0


def main():
    command = sys.argv[1] if len(sys.argv) > 1 else ""
    if command == "trace" and len(sys.argv) == 3:
        records = collect(sys.argv[2])
        print(tree(records) if records else "no calls recorded for that correlation id")
        return 0 if records else 1
    if command == "smoke":
        return Smoke().run()
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
