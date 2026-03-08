# Release Checklist

## Pre-release

- [ ] CI green on latest `main`
- [ ] Android unit tests passing
- [ ] Lint report reviewed (critical issues triaged)
- [ ] App version code and name updated
- [ ] Security-sensitive changes reviewed (crypto, auth, key handling)
- [ ] Smoke workflow passed (`release-smoke.yml`)

## Functional Smoke

- [ ] App launches on fresh install
- [ ] Chat tab sends/receives local message
- [ ] Pay tab opens without crash
- [ ] Crypto tab performs transfer and updates history
- [ ] Network tab sends and receives message
- [ ] Oracle tab fetches quote
- [ ] Wallet tab register/login succeeds

## Crypto & Network

- [ ] Encryption roundtrip test passed
- [ ] ZK proof verification test passed
- [ ] Distributed node convergence test passed
- [ ] Network replay coordinator starts on app boot

## Release Artifacts

- [ ] Debug and release APKs generated
- [ ] Checksums generated and stored
- [ ] Release notes drafted
- [ ] Tag created and pushed
- [ ] GitHub release published
