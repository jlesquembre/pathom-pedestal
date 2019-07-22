.PHONY: pom-deps deploy deploy-dry clean jar

clojars_auth := -Dclojars.password=`pass clojars | head -n 1`

pom-deps:
	clojure -Spom

deploy:
	mvn release:prepare
	git fetch
	mvn $(clojars_auth) release:perform

deploy-dry:
	mvn release:prepare -DdryRun=true

clean:
	mvn release:clean

jar:
	rm -rf target
	mvn package


deploy-snapshot: jar
	mvn $(clojars_auth) deploy
