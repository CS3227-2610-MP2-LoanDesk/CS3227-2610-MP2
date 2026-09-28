# Reflection

## Planning and skill-trigger reliability

When I completed a milestone that involved changing the `.json` storage to H2
storage, it triggered one of my previously created skills, which I had
forgotten about. This made me appreciate planning more because the skill ran a
test that I had initially wanted to do but had forgotten about as I continued
working on the project. Having skills helps to make sure that previously
planned checks are carried out even if I lose track of them as the project
progresses.

One prompt I had was:

> I just realised, earlier in the password parts, how come it didnt trigger the
> `loandesk-borrower-edge-case-test-review` but instead needed the independent
> reviewer to bring it up?

I had this question because, after an independent reviewer—which was one of my
skills—was triggered, I noticed that it had done the job that another skill I
had implemented should have been doing. This made me wonder why the other skill
had not been triggered, although there had already been tests put in place for
it. Although the issue was mitigated afterwards by the AI fixing the gap in the
process, I continued by asking why the skill was not reliably triggering,
despite the presence of tests. I learned that:

> Tests passed because they validated the code and hooks—not the skill's
> invocation timing.

The learning point was that testing a skill's internal checks does not
necessarily verify that the skill will be selected at the correct time. The
workflow therefore needs both tests for the skill's behaviour and clear
trigger conditions for when the skill should be invoked.

## Using multiple independent reviewers

When reading the prompts and results given by the subagents after a skill was
invoked, I noticed that even though they received the same prompts, they gave
me different results. This made me wonder whether my subagents could also have
blind spots and how I could mitigate them.

I considered changing the skill so that, instead of calling one subagent with a
general prompt, it would call multiple subagents and give each of them a more
specific direction. This would allow them to deep-dive into different aspects
and help identify whether the important parts were working correctly. The
prompt I gave was:

> They all gave different feedback with the same prompt. Just wondering if we
> can change the skill that calls these agents to instead of calling one and
> making it do that prompt, we can make multiple and give them more specific
> tasks so they can deep dive and help to identify if all aspects, or at least
> the more important aspects, are all working okay? Is this a good idea or do
> you think this might be too much?

I made this prompt because I was unsure whether this would be feasible and
wanted feedback on whether it was a viable option. I learned that multiple
focused reviewers can reduce blind spots, although they also add review time
and should be used when the change is meaningful enough to justify them.

## Independent review and persistence boundaries

When running my pre-PR skill, which gets independent reviewers to check for
issues, I noticed that they often found problems that I was not able to find.
For one of the reviews, I decided to clarify how the issue was possible because
it seemed impossible based on the implementation. I asked:

> How did the reviews manage to get this issue?

I realised that the agents did not only go through the GUI. They also tested
the persistence boundary directly through methods such as direct service
calls. At first, I was unsure whether this was a good way of reviewing because
users would not be able to access the application through direct service calls.

After discussing it further, I learned that this approach can still be useful.
Future changes or bugs could expose weaknesses at the service or persistence
boundary, even if the current GUI prevents users from reaching them. Fixing
these issues early provides a guardrail for future implementations.

This showed me that independent reviewers can examine the application from
angles that I cannot easily access while still giving feedback that makes the
implementation smoother and safer in the future.

## Recording and preventing recurring review gaps

As I continued working on the project, I thought about whether independent
reviewers would continue finding the same types of issues and whether there was
a way to prevent repeated mistakes. I decided to plan an improvement to the
skills using this prompt:

> Could we begin noting the problems the independent reviewers noted from now
> on as an artefact and refer to these issues as possible gaps for future
> references? In future, we could try to avoid such lapses in development. Do
> you think this should be an additional skill so that we do not have to solve
> problems that have already surfaced before?

This led to a change in the
`loandesk-borrower-change-completeness-review` skill. The skill reviews commits
and pull requests and records previous issues so that future reviews can check
for similar problems.

However, as I read the proposed change, I noticed that it was doing something
different from what I initially intended. I therefore followed up with:

> Could these problems be identified and prevented in the implementation phase
> instead of the checking phase? Wouldn't that be better than what you just
> suggested, or is your approach better?

This resulted in the implementation-preflight skill being used to read
previously identified issues before implementation. This moved some of the
learning from the review phase into the planning phase, where it could prevent
mistakes before they were introduced.

## Keeping documentation current

As I continued working on the project, I noticed that I was not updating the
User Guide and Developer Guide along the way. I thought that documentation
verification could be included in the skill used to check the readiness of a
commit or pull request. I asked:

> Could we update the skill that checks whenever we are preparing to commit or
> create a PR to also include verifying that the documentation is up to date?

I felt that as the project continued, there would be opportunities to improve
the skills further and make them more comprehensive. This would allow the
workflow to become simpler and more reliable for the developer.

## Learning from the complete workflow

As I came to completion, I felt that the amount of work I had to do
progressively decreased. I thought this might have been due to the
implementation of the skills, so I asked the AI:

> Based on this whole project so far, can you go through the workflow that we
> have been employing and teach me how I can replicate this in another project
> in the future? I feel like from milestone 4 onwards it was all very quick
> once I answered your questions. Was it the skills?

This helped me understand the workflow and how it reduced my own workload. I
learned that:

- Defining policies, ownership and scope before implementation reduces rework.
- The implementation-preflight skill identifies relevant tests, documentation
  and previous review gaps before coding.
- Review skills provide reusable checks for ownership, edge cases, UI behaviour
  and completeness.
- Recording recurring issues in a review-gap registry helps prevent previously
  discovered mistakes from reappearing.
- Reusable project structures, test patterns and UI conventions make later
  milestones faster to implement.
- Independent reviews provide fresh perspectives and expose issues that normal
  testing can miss.
- Automated tests and Git hooks reduce the amount of manual verification
  required.
- The skills do not replace my responsibility. I still have to make policy
  decisions, review generated changes, test the GUI and coordinate shared-role
  work.

Overall, I learned that a disciplined workflow can reduce workload by moving
effort earlier into planning, automation and reusable checks. These practices
made the development of later milestones simpler while helping to avoid past
mistakes.
