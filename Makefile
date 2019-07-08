.PHONY: pom-deps deploy deploy-dry clean jar

pom-deps:
	clojure -Spom

deploy:
	mvn release:prepare
	git fetch
	mvn release:perform

deploy-dry:
	mvn release:prepare -DdryRun=true

clean:
	mvn release:clean

jar:
	rm -rf target
	mvn package
