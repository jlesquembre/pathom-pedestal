.PHONY: pom-deps jar compile-viz

pom-deps:
	clojure -Spom

jar:
	rm -rf target
	clojure -Spom
	clojure -A:jar \
		-m hf.depstar.jar target/pathom-pedestal.jar -v

compile-viz:
	cd pathom-viz && npm install
	cd pathom-viz && npx shadow-cljs release standalone
	mkdir -p resources/pathom-viz
	cp -rL ./pathom-viz/standalone/assets/* resources/pathom-viz
