# Self-play and weight tuning

## Opening variation and king-threat extension

SelfPlay accepts an `openingPlies` parameter (Ant property `openingPlies`, default 0). For those first plies it samples uniformly from the engine legal moves using a seed derived from the master seed and game index. This keeps runs repeatable while avoiding the identical initial line in every color-swapped match. The option is used by tuning and validation with value 2.

The king-escape threat extension is switchable per Search agent. A paired quick check used 40 games per setting, 2,048 nodes per move, 220-ply cap, seed 20261123, and two randomized opening plies. Extension on scored 33 wins, 7 draws, 0 losses (106 tournament points); extension off scored 32 wins, 7 draws, 1 loss (103 points). Both were Search versus Greedy, with colors swapped evenly. The sample is small and the difference is not strong evidence; the extension remains enabled because it scored better in this check.

## Genetic tuner

`GeneticTuner` evolves all 17 coefficients jointly: 11 White-perspective genes and 6 Black-perspective genes. Joint evolution preserves the asymmetric evaluation while allowing interactions between both color vectors. The implementation uses a real-valued genome, a uniform arithmetic blend crossover, Gaussian mutation with generation-decaying sigma, tournament selection of size 3, and one elite individual. Each gene has a separate bound in the source. Fitness is the candidate's tournament points (3/1/0) against default weights, the previous generation's best, Greedy, Random, and the legacy seven-feature evaluator. Each opponent matchup has an even number of games, both colors, and two randomized opening plies.

The run is reproducible from its seed. Ant target `tune` parameters are `population`, `generations`, `tuneGames` (even games per opponent), `nodeBudget`, `seed`, `moveCap`, `tuneDir`, and `resume`. Each completed generation writes a population CSV, a best-weight CSV, generated Java source at `docs/tuning/champion/Weights.java`, and `stats.csv`. To resume, pass a saved population file as `-Dresume=docs/tuning/population_genN.csv`; the next generation is continued. The generated source has no runtime file dependency; incorporate its arrays into the runtime `Weights` defaults when promoting a champion.

The recorded run used population 8, 4 generations, 8 games per opponent, 512 nodes per move, move cap 180, seed 20261124. That is 40 games per candidate per generation over 5 opponents. Generation best/mean fitness values were:

| Generation | Best | Mean |
|---:|---:|---:|
| 0 | 0.7917 | 0.7469 |
| 1 | 0.7583 | 0.7198 |
| 2 | 0.7750 | 0.7104 |
| 3 | 0.8083 | 0.7115 |

The champion is generation 3 individual 2. Validation against default weights used 500 games, 1,024 nodes per move, two randomized opening plies, seed 20261125, and move cap 250. The result was 244 wins, 61 draws, and 195 losses for the champion; tournament points were 793 versus 646. Match score `(wins + 0.5 * draws) / games` was 54.9%, with an approximate 95% normal interval of 50.8% to 59.0%. By color, as White the champion scored 119/34/97; as Black it scored 125/27/98. This is a modest positive result, with the approximate interval only narrowly above 50%; it is not evidence of a large practical advantage. Validation searched 27,796,731 total nodes, completed about 132,340 games/hour in this environment, and cross-checked 8 games against `GameAshtonTablut`.

The run was intentionally small enough for a quick course-work iteration. Fitness is noisy at this game count and node budget; increase games per opponent and node budget for later finalists before treating these weights as a robust tournament improvement. The current runtime `Weights` defaults remain unchanged; the exported champion is an experiment artifact until deliberately promoted.
