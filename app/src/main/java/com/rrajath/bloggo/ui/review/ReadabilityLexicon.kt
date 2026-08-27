package com.rrajath.bloggo.ui.review

/**
 * The word lists behind the readability review, ported one to one from the
 * prototype script (`internal/design/bloggo-prototype.html`, the `LY_STOP` /
 * `WEAK_WORDS` / `COMPLEX` / `COMPLEX_PHRASES` / `PARTICIPLES` constants).
 *
 * Kept as Kotlin constants rather than bundled asset files: the lists are small
 * and static, the app has no asset-loaded-data anywhere else, and keeping them
 * here is what lets [ReadabilityAnalyzer] stay a pure object with no Android
 * context. Per DESIGN_SYSTEM.md §11 these lists and the thresholds in
 * [ReadabilityAnalyzer] are the parts to review when tuning the feature.
 */
internal object ReadabilityLexicon {

  /** `-ly` words that are not adverbs, so the adverb test skips them. */
  val LY_STOP: Set<String> = setOf(
    "only", "family", "reply", "apply", "supply", "early", "fly", "ally", "rely", "holy", "ugly",
    "jelly", "belly", "silly", "hilly", "folly", "rally", "tally", "sally", "anomaly", "assembly",
    "monopoly", "multiply", "imply", "comply", "july", "likely", "lonely", "lovely", "friendly",
    "lively", "costly", "deadly", "daily", "weekly", "monthly", "yearly", "timely", "orderly",
    "elderly", "homely", "kindly", "measly", "wobbly", "bubbly", "gnarly", "curly", "burly",
    "surly", "pearly", "gully", "bully", "wholly",
  )

  /** Weak qualifiers, shown under the same wash as adverbs. */
  val WEAK_WORDS: Set<String> = setOf(
    "very", "really", "quite", "rather", "somewhat", "just", "actually", "basically", "simply",
    "fairly", "pretty", "totally", "definitely", "certainly", "probably", "essentially",
    "virtually", "extremely", "highly", "literally", "honestly", "obviously", "truly", "slightly",
    "relatively", "particularly", "especially", "arguably", "somehow", "kind", "sort",
  )

  /** Single complex words mapped to a plainer choice. `""` means: just cut it. */
  val COMPLEX: Map<String, String> = mapOf(
    "utilize" to "use", "utilise" to "use", "utilizes" to "uses", "utilized" to "used",
    "utilizing" to "using", "commence" to "start", "commenced" to "started", "endeavour" to "try",
    "endeavor" to "try", "numerous" to "many", "sufficient" to "enough", "additional" to "extra",
    "facilitate" to "help", "leverage" to "use", "demonstrate" to "show", "demonstrates" to "shows",
    "demonstrated" to "showed", "approximately" to "about", "modify" to "change",
    "component" to "part", "components" to "parts", "individual" to "person", "initial" to "first",
    "attempt" to "try", "regarding" to "about", "assist" to "help", "obtain" to "get",
    "require" to "need", "requires" to "needs", "purchase" to "buy", "therefore" to "so",
    "furthermore" to "also", "moreover" to "also", "nevertheless" to "still",
    "consequently" to "so", "subsequently" to "later", "ascertain" to "find out",
    "terminate" to "end", "initiate" to "begin", "indicate" to "show", "indicates" to "shows",
    "aforementioned" to "", "notwithstanding" to "despite", "accordingly" to "so",
    "henceforth" to "from now on",
  )

  /** Wordy phrases mapped to a plainer choice. `""` means: just cut it. */
  val COMPLEX_PHRASES: List<Pair<String, String>> = listOf(
    "in order to" to "to", "a number of" to "some", "the fact that" to "that",
    "in the event that" to "if", "with regard to" to "about", "with respect to" to "about",
    "due to the fact that" to "because", "in spite of the fact that" to "although",
    "is able to" to "can", "are able to" to "can", "was able to" to "could",
    "were able to" to "could", "a majority of" to "most", "at this point in time" to "now",
    "referred to as" to "called", "for the purpose of" to "for", "in the near future" to "soon",
    "in the process of" to "",
  )

  /** Irregular past participles the `be` + participle passive test looks for. */
  val PARTICIPLES: Set<String> = setOf(
    "done", "gone", "seen", "made", "given", "taken", "known", "shown", "written", "said", "held",
    "kept", "told", "felt", "found", "built", "sent", "brought", "thought", "caught", "bought",
    "put", "set", "cut", "become", "begun", "chosen", "driven", "drawn", "eaten", "fallen",
    "forgotten", "hidden", "lost", "met", "paid", "read", "sold", "spent", "stood", "understood",
    "won", "broken", "spoken", "stolen", "torn", "worn", "heard", "left", "meant", "sung",
  )
}
