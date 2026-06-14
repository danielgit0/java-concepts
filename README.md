# Java Concepts

> **DISCLAIMER 1**: The files under docs that describe problems and the proposed solutions were generated with Claude, DeepSeek, ChatGPT and Gemini as a guideline to study.
> During the actual implementation it was discovered that some test cases were irrelevant as they
> were not properly testing concurrency in the FinTech context.
> A good example is, that in `06_java_coding_problems.md - Problem 1 - Stage 2`, the tests will always pass (with or
> without concurrency) as the assertion was to ensure that the `balance` was never below 0.
> This was hiding the actual concurrency issues that could lead to "artificially" created money
> or the lost of it leading to, which is unacceptable in any context.
> The correct way is to check the consistency of the `balance`.
> As the progress goes on, it could be that similar inconsistencies appear.
> Currently, it is not intended to correct the Markdow, let it be a reminder to ALWAYS verify the outputs of any LLM.

> **DISCLAIMER 2**: To be able to show the improvements, the classes are copied and then extended
> accordingly. It is clear that it is not 100% TDD.

Practicing concurrency and multithreading concepts with plain Java.

## License
MIT
