<div class="wide-table" markdown>

| Order lines | JSON bytes | Format | Bytes | vs JSON | Encode µs | Decode µs | Extra µs vs JSON | Break-even link speed |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 133 | json | 133 | 100% | 0.2 | 0.4 | 0.0 | - |
| 1 | 133 | gzip | 131 | 98% | 2.9 | 2.1 | 4.4 | 0.5 MB/s (4 Mbit/s) |
| 1 | 133 | bson | 152 | 114% | 0.4 | 0.4 | 0.2 | never (no bytes saved) |
| 1 | 133 | bson+gzip | 143 | 108% | 3.1 | 2.1 | 4.7 | never (no bytes saved) |
| 4 | 235 | json | 235 | 100% | 0.3 | 0.5 | 0.0 | - |
| 4 | 235 | gzip | 150 | 64% | 3.2 | 2.3 | 4.7 | 18.1 MB/s (145 Mbit/s) |
| 4 | 235 | bson | 278 | 118% | 0.6 | 0.5 | 0.3 | never (no bytes saved) |
| 4 | 235 | bson+gzip | 176 | 75% | 3.8 | 2.5 | 5.4 | 10.8 MB/s (86 Mbit/s) |
| 10 | 439 | json | 439 | 100% | 0.6 | 0.8 | 0.0 | - |
| 10 | 439 | gzip | 172 | 39% | 3.6 | 2.8 | 5.0 | 53.1 MB/s (425 Mbit/s) |
| 10 | 439 | bson | 530 | 121% | 1.0 | 1.0 | 0.5 | never (no bytes saved) |
| 10 | 439 | bson+gzip | 209 | 48% | 4.4 | 3.1 | 6.1 | 37.6 MB/s (301 Mbit/s) |
| 40 | 1,459 | json | 1,459 | 100% | 1.8 | 2.6 | 0.0 | - |
| 40 | 1,459 | gzip | 264 | 18% | 6.6 | 4.9 | 7.1 | 168.8 MB/s (1,350 Mbit/s) |
| 40 | 1,459 | bson | 1,820 | 125% | 3.0 | 3.3 | 1.9 | never (no bytes saved) |
| 40 | 1,459 | bson+gzip | 385 | 26% | 8.8 | 5.8 | 10.2 | 105.2 MB/s (842 Mbit/s) |
| 150 | 5,220 | json | 5,220 | 100% | 6.6 | 10.3 | 0.0 | - |
| 150 | 5,220 | gzip | 591 | 11% | 17.2 | 14.0 | 14.3 | 323.6 MB/s (2,589 Mbit/s) |
| 150 | 5,220 | bson | 6,621 | 127% | 10.8 | 11.8 | 5.7 | never (no bytes saved) |
| 150 | 5,220 | bson+gzip | 1,006 | 19% | 25.1 | 16.2 | 24.4 | 172.5 MB/s (1,380 Mbit/s) |

</div>
