<div class="wide-table" markdown>

| Databases | Cache | Parallel | Follow-ups | modulith | macro-modulith | loopback-codec | loopback-http | groups | split-dbs | microservices | microservices-macro |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| shared | on | on | outbox | 450 | 444 | 449 | 326 | 245 | - | 161 | 161 |
| shared | on | on | events | 433 | 449 | 425 | 315 | 239 | - | 151 | 150 |
| shared | on | off | outbox | 446 | 452 | 449 | 313 | 229 | - | 140 | 139 |
| shared | on | off | events | 429 | 432 | 419 | 305 | 223 | - | 133 | 133 |
| shared | off | on | outbox | 430 | 431 | 429 | 293 | 232 | - | 136 | 136 |
| shared | off | on | events | 401 | 404 | 402 | 278 | 226 | - | 131 | 130 |
| shared | off | off | outbox | 408 | 412 | 398 | 258 | 214 | - | 116 | 114 |
| shared | off | off | events | 387 | 382 | 377 | 253 | 212 | - | 112 | 111 |
| split | on | on | outbox | - | - | - | - | 245 | 255 | 159 | 157 |
| split | on | on | events | - | - | - | - | 251 | 247 | 150 | 150 |
| split | on | off | outbox | - | - | - | - | 238 | 235 | 139 | 135 |
| split | on | off | events | - | - | - | - | 229 | 238 | 132 | 131 |
| split | off | on | outbox | - | - | - | - | 215 | 232 | 135 | 132 |
| split | off | on | events | - | - | - | - | 226 | 229 | 130 | 129 |
| split | off | off | outbox | - | - | - | - | 213 | 208 | 114 | 114 |
| split | off | off | events | - | - | - | - | 207 | 204 | 111 | 110 |

</div>
