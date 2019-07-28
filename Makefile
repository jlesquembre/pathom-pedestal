.PHONY: pom-deps deploy deploy-dry clean jar compile-viz

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

compile-viz:
	cd pathom-viz && npx shadow-cljs release standalone
	mkdir -p resources/pathom-viz
	cp -rL ./pathom-viz/standalone/assets/* resources/pathom-viz
