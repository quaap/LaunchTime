.PHONY: build database

build:
	./gradlew build

lint:
	./gradlew updateLintBaseline

install:
	adb install -r app/build/outputs/apk/debug/app-debug.apk

log:
	adb logcat -s Categories:D

database:
	@database/fdroid.sh
	@cat database/local.txt >> app/src/main/res/raw/apps_categories.csv
	@database/kaggle.sh database/kaggle.csv >> app/src/main/res/raw/apps_categories.csv
	@sort app/src/main/res/raw/apps_categories.csv | uniq -i > app/src/main/res/raw/apps_categories_tmp.csv
	@cat app/src/main/res/raw/apps_categories_tmp.csv | grep -v "not found in databases" > app/src/main/res/raw/apps_categories.csv
	@rm app/src/main/res/raw/apps_categories_tmp.csv


