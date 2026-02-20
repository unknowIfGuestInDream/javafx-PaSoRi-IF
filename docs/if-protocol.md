# IF Communication Protocol

## Message Format

| D0  | D1      | D2  | D3      | ...       | Dn-1 | Dn  |
|-----|---------|-----|---------|-----------|------|-----|
| STX | CMD/RES | LEN | Data(0) | Data(m)   | BCC  | ETX |

- **STX** = 0x02, **ETX** = 0x03
- **BCC**: XOR of D1 through Dn-1, ensures the result is 0x00
- **LEN**: Byte count of Data(0) through Data(m)

## Command List

| No | Command      | CMD/RES   | Description                                        |
|----|--------------|-----------|----------------------------------------------------|
| 1  | Open         | 0x10/0x11 | Start NFC relay — turn NFC carrier ON              |
| 2  | Close        | 0x20/0x21 | Stop NFC relay — turn NFC carrier OFF              |
| 3  | CardAccess   | 0x30/0x31 | Send/receive FeliCa data link layer packets to card |
| 4  | SetParameter | 0x40/0x41 | Configure NFC relay parameters                     |

## CardAccess Command Routing

The CardAccess command analyzes the FeliCa data link layer command code to determine the operation:

- **Polling (0x04)**: Uses SDK `polling_and_get_card_information()` to detect cards, returns IDm + PMm in FeliCa data link layer response format
- **Other commands** (Read Without Encryption 0x06, Write Without Encryption 0x08, etc.): Uses SDK `thru()` to forward raw FeliCa data to the card and return the response

## Exception Responses (0xF1)

| Error Source (Data[0]) | Error Code (Data[1]) | Description                                     |
|------------------------|----------------------|-------------------------------------------------|
| 0x30                   | 0x01                 | CardAccess sent before Open command              |
| 0x30                   | 0x10                 | Card response timeout                            |
