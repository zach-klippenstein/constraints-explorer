# Releasing constraints-explorer

1. Update version in `gradle.properties` (remove SNAPSHOT). 
2. Add entry to `CHANGELOG.md`. 
3. `git commit -am "Releasing vX.Y.Z"`
4. `./gradlew publishAndReleaseToMavenCentral --no-configuration-cache`
5. `git tag vX.Y.Z`
6. Update version in `gradle.properties` (increment and add SNAPSHOT).
7. `git commit -am "Finish releasing vX.Y.Z"`
8. `git push && git push --tags`
9. Copy changelog entry into new release on Github repo.
