# 04a — Set up the build before any tests or code

**Slide:** Writing Tests (setup, off-slide)
**Mode:** normal. This is the one step that may change `pom.xml`, the Maven wrapper and the lint config. Run it once, commit it, then never touch those files again without asking.

```text
Set up the SubTrack build from SPEC.md section 1. Write no tests and no
endpoint code.

1. A Maven project: Java 17, Spring Boot 3, the Maven wrapper (./mvnw),
   package root edu.columbia.subtrack. Dependencies: spring-boot-starter-web,
   H2 (runtime) and spring-boot-starter-test (test). Add nothing else.
2. A SubTrackApplication class so the app starts, and nothing more.
3. ./mvnw verify must also run JaCoCo, Checkstyle and PMD:
   - JaCoCo only reports coverage. Set no minimum and never fail the
     build on it.
   - Checkstyle and PMD fail the build on a violation. Use standard
     rulesets, and exclude src/test from Checkstyle's method-name rule,
     because test names look like R2_billingDateKeepsMonthEndAnchor.
4. Put the Checkstyle and PMD config files under config/.

When ./mvnw -q verify passes on the empty project, stop and give me:
- the files you created,
- where each report is written (JaCoCo, Checkstyle, PMD),
- the verify output.
```

**What to point out live:** this is the only prompt that touches the build. Everything after it is told the build already works, so a failing `verify` always means the code or a test is wrong, never the setup.
