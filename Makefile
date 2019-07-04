.PHONY: pom-deps deploy deploy-dry clean jar

pom-deps:
	clojure -Spom

deploy:
	clojure -Spom
	mvn release:prepare
	git fetch

deploy-dry:
	clojure -Spom
	mvn release:prepare -DdryRun=true

clean:
	mvn release:clean

jar:
	rm -r target
	mvn package
