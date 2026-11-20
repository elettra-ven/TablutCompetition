# Ashton rules ground truth

This note records the behavior of the checked-in professor code. The implementation in `GameAshtonTablut` is authoritative for Ashton games; slides are descriptive only.

## Representation and protocol

- `StateTablut` stores a 9 by 9 `Pawn[][]`, indexed `[row][column]`, with row 0 at the top and column 0 at the left. `State.getBox(row, column)` emits lowercase file plus one-based rank (`a1` through `i9`); `Action` parses those coordinates case-insensitively for the file. See `StateTablut()`, `State.getBox`, and `Action.getRow/Column*`.
- Pawn encodings are `O` (EMPTY), `W`, `B`, `K`, `T` (THRONE), in `State.Pawn`. The center is initially `K`, not `T`. Camps are not marked in the board: unoccupied camp squares are `EMPTY`, and camp membership is an internal list of coordinates in `GameAshtonTablut`.
- `StateTablut()` constructs the standard 8 white soldiers, king, and 16 black soldiers, but sets turn to BLACK. `Server.run()` resets the Ashton initial state's turn to WHITE before sending it.
- `TablutClient` connects to port 5800 for White and 5801 for Black; it sends a JSON name first, then reads a JSON `StateTablut` and writes JSON `Action` records through `StreamUtils`. The server sends the resulting state to both clients after every submitted move, so the client blocks on `read()` during opponent turns.

## Move legality and movement

`GameAshtonTablut.checkMove(state, action)` validates and mutates the supplied state in place, returning that same resulting state. Callers needing a non-mutating probe must pass a clone. Validation order includes coordinate bounds, destination not THRONE and EMPTY, citadel destination/source restrictions, no stationary move, correct side's piece, orthogonality, and path obstruction. It does not validate `Action.turn`; `Server` overwrites that field with the state's turn. The specific checked exceptions are declared by `Game.checkMove`.

Movement is orthogonal and may span multiple empty squares. Any non-empty square blocks traversal, including a king and a THRONE. The king may leave the center: `movePawn` restores `T` at e5 when the moving piece started there. Consequently there is no legal route through an empty throne in normal play: it is restored as THRONE as soon as vacated, and `checkMove` never allows a destination equal to THRONE.

Citadels (the implementation's term for camps) are exactly `a4,a5,a6,b5,d1,e1,f1,e2,i4,i5,i6,h5,d9,e9,f9,e8`. A move ending in a citadel is rejected if its origin is not a citadel. From a citadel origin, the code allows a citadel destination only when the absolute row/column displacement is at most 5; traversal checks similarly permit citadel squares while the origin is itself a citadel. This is a coordinate rule, not a remembered "has left camp" flag: once the piece starts outside, it cannot cross or enter a citadel. The code does not distinguish which of the four camps a piece belongs to.

## Captures

Capture checks are triggered only after the moving piece has moved; therefore a piece entering a sandwiched position is not removed until a later move creates a capture. All four orthogonal directions from the destination are examined, allowing multiple captures on one move.

- White captures black soldiers when the adjacent square contains B and the square beyond contains W, K, or T, or is a citadel other than the four citadels immediately adjacent to the board center (`a5,e1,i5,e9`). The four center-adjacent camp squares thus are not hostile for this capture check. Empty camp cells elsewhere are hostile; occupancy is not tested for citadel hostility.
- Black captures white soldiers when the adjacent square contains W and the square beyond contains B, T, e5, or any citadel. These tests use fixed board edges to avoid indexing off-board. The e5 check is present even though e5 normally contains either K or T.
- Black's king capture is not a general adjacent-count rule. Four separate direction-specific methods use hard-coded neighboring cells. On e5, B must occupy all three non-attacking orthogonal neighbors. On e4, d5, f5, and e6 (the four adjacent throne squares), the code checks two specific black cells, with the third side treated as satisfied by the throne. Everywhere else it declares capture when the opposite square contains B or is any citadel; it does not require the other side to be surrounded. This exactly follows those methods, including their coordinate-specific checks.
- No rule captures the last black soldier as a win condition; the source explicitly leaves this as TODO. No general king-side capture of soldiers occurs: only black's capture routines handle the king.

## End conditions and repetition

- After a White move, `checkCaptureWhite` declares WHITEWIN if the moved piece is the king and its destination is any board edge. It does not test the slide's marked escape set: corners and camp squares on the edge also win. There is no separate generic "king on escape" check elsewhere in the server loop.
- Black king capture sets BLACKWIN in the capture routines above. `Server` otherwise ends a game only for too many invalid moves, elapsed whole-game limit, or a terminal `Turn` produced by the game. The checked-in Ashton path has no no-legal-moves detection, so a player with no legal move is not automatically declared loser by the server.
- `GameAshtonTablut` owns `drawConditions` as an instance `ArrayList<State>`, compares full state equality (board and side to move), and appends a clone after each accepted move. It counts matching prior entries and declares DRAW only when matches are strictly greater than `repeated_moves_allowed`. `-s` configures this threshold (`Server.main`, default 0); with zero, the first repeat draws. `-c` configures retained history size (`Server.main`, default -1 = unlimited). When nonnegative, the oldest entry is removed if the current size is greater than the limit, before the next clone is added.
- `movesWithutCapturing` starts at zero. Any soldier capture resets it to -1 and the post-move increment makes it zero; then `checkMove` clears the repetition list. The same applies to captures detected by the black king checks only insofar as the code paths update the counter: king capture does not reset it. This counter is per oracle instance, not static.
- The random client constructs `new GameAshtonTablut(99, 0, "garbage", "fake", "fake")`: `99` means a state must recur more than 99 times before draw; `0` means unlimited history (the constructor's `cache_size` logic removes one only when size > 0). It is a single oracle instance for that random client's run, and its probes are made on live states in the checked-in client, so its mutations/history can contaminate later candidate checks. The server instead passes `repeated` and `cacheSize` from its options.

## Side effects and exceptions

Each `GameAshtonTablut` constructor creates a timestamped file beneath the supplied logs folder, attaches a `FileHandler` to the named global Java logger `GameLog`, and logs the initial state. Constructor I/O failure prints a stack trace and calls `System.exit(1)`. `checkMove` also prints every accepted resulting state to stdout and writes warnings for rejected moves. The lists/counters are per instance, but the named logger is process-global and handlers accumulate across instances. Do not use this oracle in a search hot path.

`ActionException` means malformed action format; `BoardException` out of bounds; `ThroneException` destination is throne; `OccupitedException` destination occupied; `CitadelException` disallowed camp destination; `StopException` no movement; `PawnException` wrong side's piece; `DiagonalException` diagonal move; `ClimbingException` piece/throne blocks the path; `ClimbingCitadelException` disallowed camp traversal. The misspelled `OccupitedException` is the source class name. `GameAshtonTablut` checks destination occupancy before side, geometry, and path, so the first applicable exception follows its validation order.

## Discrepancies from the slide summary

1. The server sets White to move initially, although `StateTablut()` alone defaults to Black.
2. Any edge square, including corners and edge camp squares, wins for a king. This differs from the marked escape cells described in the slides.
3. King capture uses direction-specific hard-coded patterns. Outside the throne and its four neighbors, one opposite black piece or citadel is sufficient; this differs from ordinary two-sided custodial capture. At adjacent throne cells, exact two-cell patterns are checked.
4. White's hostility treatment exempts four center-adjacent citadels, while Black treats every citadel as hostile for soldier capture. The slide summary implies a uniform camp rule.
5. Camp entry/leaving is determined from the move's source coordinate, without camp identity or a persistent "left" state; starting in any citadel enables citadel traversal and movement among citadels within the displacement limit.
6. The Ashton server does not detect no-legal-move losses and does not implement the slides' marked-escape restriction.
7. A repeated position draws after matches exceed the configured threshold; captures clear the stored history. The slide's simple "same state twice" does not describe these controls.
8. No win is declared for capturing the last black soldier.

The engine and tests should reproduce the source behavior above, including apparent bugs, unless a later course rule update explicitly changes the ground truth.
