# RTS-unsafety in the context of Dependency Injection

| ID | Ursache                                    | Framework          | Beispielprojekt                    |
| -- | ------------------------------------------ | ------------------ | ---------------------------------- |
| S1 | XML                                        | Spring             | Spring_xml                         |
| S2 | Code of a bean has changed                 | Spring             | Spring_changed                     |
| S3 | Prio + AutoConfiguration                   | Spring             | Spring_higher_prio_autoconfig      |
| S4 | Prio + ComponentScan                       | Spring             | Spring_higher_prio_component_scan  |
| S5 | Collection Injection                       | Spring             | Spring_collection                  |
| G1 | Collection Injection via AutobindSingleton | Guice + Governator | Guice_autobindsingleton_collection |
| G2 | Collection Injection via Multibindings     | Guice              | Guice_multibindings_collection     |
| C1 | XML                                        | CDI + Weld         | CDI_xml                            | 
| C2 | Prio                                       | CDI + Weld         | CDI_higher_prio                    |

## Versions
- Ekstazi: 5.3.0
- HyRTS: 1.0.1
- STARTS: 1.4-SNAPSHOT (**built from source**, 14.11.21, commit-sha: e1d29be2958ec27fac12e6c8611577fce5a73e40)
- DIRTS: 0.1-SNAPSHOT

- JUnit: 4.12


:ballot_box_with_check: -> Recognized properly

:black_square_button:   -> Not recognized

| ID | Ekstazi                  | HyRTS                   | STARTS                  |
| -- | ------------------------ | ----------------------- | ----------------------- |
| S1 | :black_square_button:    | :black_square_button:   | :black_square_button:   |
| S2 | :ballot_box_with_check:  | :ballot_box_with_check: | :black_square_button:   |
| S3 | :black_square_button: *1 | :black_square_button:   | :black_square_button:   |
| S4 | :black_square_button:    | :black_square_button:   | :black_square_button:   |
| S5 | :black_square_button: *1 | :black_square_button:   | :black_square_button:   |
| G1 | :black_square_button:    | :black_square_button:   | :black_square_button:   |
| G2 | :ballot_box_with_check:  | :black_square_button:   | :ballot_box_with_check: |
| C1 | :black_square_button:    | :black_square_button:   | :black_square_button:   |
| C2 | :black_square_button: *2 | :black_square_button:   | :black_square_button:   |

*1 The whole class annotated with `@Configuration`, adding the bean annotated with `@Primary` has to be added  (e.g., commented in).

*2 The whole class annotated with `@Alternative` and higher `@Priority`  has to be added (e.g., commented in).
