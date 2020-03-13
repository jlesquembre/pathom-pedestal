.PHONY: pom-deps deploy deploy-dry clean jar compile-viz

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

compile-viz:
	cd pathom-viz && npm install
	cd pathom-viz && npx shadow-cljs release standalone
	mkdir -p resources/pathom-viz
	cp -rL ./pathom-viz/standalone/assets/* resources/pathom-viz

deploy-snapshot: jar
	mvn $(clojars_auth) deploy
