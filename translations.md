# Translations contribution guide

You can help with the translation of SCP Wallet in any language, it will be much appreciated.

It is requested a good skill level on both English and the language you're translating to (translations done using automatic tools will not be accepted).

You can use any text editor to perform the translation, but I suggest you to use a program with XML syntax highlighting.

## Steps

If you don't know how to use git, just translate the file [strings.xml](app/src/main/res/values/strings.xml) following the guidelines below, then contact me and send me the translated file. I'll open a pull request for you.

If you know how to use git:
1. Clone the repo
2. Create a new branch called translation-{language} (for example "translation-italian")
2. Create a new folder inside [app/src/main/res](app/src/main/res) using the [two letter language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) (ISO 639-1) you're translating to, for example: "values-it" for italian, "values-es" for spanish, "values-fr" for french.
3. Copy the file [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml) into the folder you've just created
4. Translate the file (see the guidelines below)
5. Commit and push the changes
6. Open a pull request

I'll review the translation and merge it or ask for changes if needed.

## Translation Guidelines

Please follow these guidelines when translating the strings.xml file:
* Don't translate comments (the ones that look like these: < ! -- this is a comment -- >)
* Don't change the strings name, but only the value. For example the row "< string name="button_save" >Save< / string >" should become in italian "< string name="button_save" >Salva< / string >". Notice that button_save isn't changed.
* Maintain the formatting of the english strings when possible. For example if a string ends with a period, the translation should also end with a period. If a string starts with a capital letter, the translation should also start with a capital letter if it makes sense in the language you're translating to.
* Treat variables carefully. Some strings contain variables that look like this "%1$s". For example "%1$s minutes ago" will be displayed as "10 minutes ago" or "25 minutes ago" to the user. Position them where it makes most sense in the translation.
* Special characters. You can find [here](https://developer.android.com/guide/topics/resources/string-resource#escaping_quotes) a list of characters that must be escaped and how to escape them. For example the single quote ' should be escaped with a backslash \\'
* New lines characters. You will find newline characters "\\n", try to use them in the same way they are used.
* Partial translations will not be merged, although you can still push them, maybe someone else will complete them

## Translation updates

With time, as new features are added to SCP Wallet, translations of existing languages will need to be updated.

They will be published in English initially until someone updates that language.

So contribution to existing languages that are lagging behind are really welcomed as well.

## Play store listing translation

If you decide to translate SCP Wallet in another language, please consider also translating the Play store listing information.

It will help users to find the app and to immediately know that their language is supported.

[Here](play-store.md) you can find the current Play store info, just add an entry with the new language to the file, I'll update it on the store afterwards.
