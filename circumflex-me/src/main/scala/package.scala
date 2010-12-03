package ru.circumflex

import java.util.Random
import java.util.regex.Pattern
import collection.mutable.HashMap

package object me {
  val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  val keySize = 20
  val rnd = new Random

  object regexes {
    val lineEnds = Pattern.compile("\\r\\n|\\r")
    val blankLines = Pattern.compile("^ +$", Pattern.MULTILINE)
    val blocks = Pattern.compile("\\n{2,}")
    val htmlNameExpr = "[a-z][a-z0-9\\-_:.]*?\\b"
    val inlineHtmlStart = Pattern.compile("^<(" + htmlNameExpr + ").*?(/)?>",
      Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    val linkDefinition = Pattern.compile("^ {0,3}\\[(.+?)\\]: *(\\S.*?)" +
        "(\\n *\"(.+?)\")?(?=\\n+|\\Z)", Pattern.MULTILINE)
    val blockSelector = Pattern.compile("(?<=\\A.*?) *\\{(\\#[a-z0-9_-]+)?((?:\\.[a-z0-9_-]+)+)?\\}(?=\\Z|\\n)", 
      Pattern.CASE_INSENSITIVE)
    val listItemSplit = Pattern.compile("\\n+(?=\\S)")

    // escape patterns

    val e_amp = Pattern.compile("&(?!#?[xX]?(?:[0-9a-fA-F]+|\\w+);)")
    val e_lt = Pattern.compile("<(?![a-z/?\\$!])")

    // deterministic patterns

    val d_code = Pattern.compile("(?: {4,}.*\\n?)+", Pattern.MULTILINE)
    val d_hr = Pattern.compile("^-{3,} *$")
    val d_ol = Pattern.compile("^\\d+\\. .*", Pattern.DOTALL)
    val d_table = Pattern.compile("^-{3,}>?\\n(.*)\\n *-{3,}$", Pattern.DOTALL)
    val d_heading = Pattern.compile("^(\\#{1,6}) (.*) *\\#*$", Pattern.DOTALL)
    val d_h1 = Pattern.compile("^(.+)\\n=+\\n?$", Pattern.DOTALL)
    val d_h2 = Pattern.compile("^(.+)\\n-+\\n?$", Pattern.DOTALL)

    // trimming patterns

    val t_blockquote = Pattern.compile("^ *>", Pattern.MULTILINE)
    val t_div = Pattern.compile("^ *\\|", Pattern.MULTILINE)
    val t_ul = Pattern.compile("^(\\* +)")
    val t_ol = Pattern.compile("^(\\d+\\. +)")
    val t_dl = Pattern.compile("^(: +)")

    protected val outdentMap = new HashMap[Int, Pattern]()
    protected val placeholder = Pattern.compile("^", Pattern.MULTILINE)

    def outdent(level: Int): Pattern = outdentMap.get(level) match {
      case Some(p) => p
      case _ =>
        val p = Pattern.compile("^ {0," + level + "}", Pattern.MULTILINE)
        outdentMap += (level -> p)
        p
    }
  }
}