// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.lex

import org.scalatest.FunSuite
import org.nlogo.api, api.{ Token, TokenType }

class TokenizerTests extends FunSuite {

  val tokenizer = new Tokenizer
  import tokenizer._

  def tokenize(s: String) = {
    val result = tokenizer.tokenize(s, "")
    expectResult(TokenType.EOF)(result.last.tpe)
    result.toList.dropRight(1)
  }
  def tokenizeRobustly(s: String) = {
    val result = tokenizer.tokenizeRobustly(s)
    expectResult(TokenType.EOF)(result.last.tpe)
    result.toList.dropRight(1)
  }
  def firstBadToken(tokens: Seq[Token]) =
    tokens.find(_.tpe == TokenType.Bad)
  ///
  test("TokenizeSimpleExpr") {
    val expected = "Token(__ignore,Ident,__IGNORE)" +
      "Token(round,Ident,ROUND)" +
      "Token(0.5,Constant,0.5)"
    expectResult(expected)(
      tokenize("__ignore round 0.5").mkString)
  }
  test("TokenizeSimpleExprWithInitialWhitespace") {
    val tokens = tokenize("\n\n__ignore round 0.5")
    val expected =
      "Token(__ignore,Ident,__IGNORE)" +
        "Token(round,Ident,ROUND)" +
        "Token(0.5,Constant,0.5)"
    expectResult(expected)(tokens.mkString)
  }
  test("TokenizeSimpleExprWithInitialReturn") {
    val tokens = tokenize("\r__ignore round 0.5")
    val expected =
      "Token(__ignore,Ident,__IGNORE)" +
        "Token(round,Ident,ROUND)" +
        "Token(0.5,Constant,0.5)"
    expectResult(expected)(tokens.mkString)
  }
  test("TokenizeIdent") {
    val tokens = tokenize("foo")
    val expected = "Token(foo,Ident,FOO)"
    expectResult(expected)(tokens.mkString)
  }
  test("TokenizeQuestionMark") {
    val tokens = tokenize("round ?")
    val expected =
      "Token(round,Ident,ROUND)" +
        "Token(?,Ident,?)"
    expectResult(expected)(tokens.mkString)
  }
  test("TokenizeBreedOwn") {
    val tokens = tokenize("mice-own")
    val expected =
      "Token(mice-own,Keyword,MICE-OWN)"
    expectResult(expected)(tokens.mkString)
  }
  test("TokenizeUnknownEscape") {
    val tokens = tokenizeRobustly("\"\\b\"")
    expectResult(0)(firstBadToken(tokens).get.startPos)
    expectResult(4)(firstBadToken(tokens).get.endPos)
    expectResult("Illegal character after backslash")(
      firstBadToken(tokens).get.value)
  }
  test("TokenizeWeirdCaseWithBackSlash") {
    val tokens = tokenizeRobustly("\"\\\"")
    expectResult(0)(firstBadToken(tokens).get.startPos)
    expectResult(3)(firstBadToken(tokens).get.endPos)
    expectResult("Closing double quote is missing")(
      firstBadToken(tokens).get.value)
  }
  test("TokenizeBadNumberFormat1") {
    val tokens = tokenizeRobustly("1.2.3")
    expectResult(0)(firstBadToken(tokens).get.startPos)
    expectResult(5)(firstBadToken(tokens).get.endPos)
    expectResult("Illegal number format")(
      firstBadToken(tokens).get.value)
  }
  test("TokenizeBadNumberFormat2") {
    val tokens = tokenizeRobustly("__ignore 3__ignore 4")
    expectResult(9)(firstBadToken(tokens).get.startPos)
    expectResult(18)(firstBadToken(tokens).get.endPos)
    expectResult("Illegal number format")(
      firstBadToken(tokens).get.value)
  }
  test("TokenizeLooksLikePotentialNumber") {
    val tokens = tokenize("-.")
    val expected = "Token(-.,Ident,-.)"
    expectResult(expected)(tokens.mkString)
  }

  test("validIdentifier") {
    assert(tokenizer.isValidIdentifier("foo"))
  }
  test("invalidIdentifier1") {
    assert(!tokenizer.isValidIdentifier("foo bar"))
  }
  test("invalidIdentifier2") {
    assert(!tokenizer.isValidIdentifier("33"))
  }
  test("invalidIdentifier3") {
    assert(!tokenizer.isValidIdentifier("color"))
  }
  // check extension primitives
  test("extensionCommand") {
    val extensionManager = new api.DummyExtensionManager {
      class DummyCommand extends api.Command {
        def getAgentClassString = ""
        def getSyntax: api.Syntax = null
        def getSwitchesBoolean = true
        def newInstance(name: String): api.Command = null
        def perform(args: Array[api.Argument], context: api.Context) {}
      }
      override def anyExtensionsLoaded = true
      override def replaceIdentifier(name: String): api.Primitive =
        if (name.equalsIgnoreCase("FOO")) new DummyCommand else null
    }
    expectResult("Token(foo,Ident,FOO)")(
      tokenizer.tokenizeForColorization("foo").mkString)
    expectResult("Token(foo,Command,FOO)")(
      tokenizer.tokenizeForColorization("foo", extensionManager).mkString)
  }
  // the method being tested here is used by the F1 key stuff - ST 1/23/08
  test("GetTokenAtPosition") {
    expectResult("Token(ask,Ident,ASK)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 0).toString)
    expectResult("Token(ask,Ident,ASK)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 1).toString)
    expectResult("Token(ask,Ident,ASK)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 2).toString)
    expectResult("Token([,OpenBracket,null)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 12).toString)
    expectResult("Token(set,Ident,SET)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 13).toString)
    expectResult("Token(set,Ident,SET)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 14).toString)
    expectResult("Token(blue,Constant,105.0)")(
      tokenizer.getTokenAtPosition("ask turtles [set color blue]", 24).toString)
  }
  // bug #88
  test("GetTokenAtPosition-bug88") {
    expectResult("Token(crt,Ident,CRT)")(
      tokenizer.getTokenAtPosition("[crt", 1).toString)
  }
  // bug #139
  test("GetTokenAtPosition-bug139") {
    expectResult("Token(crt,Ident,CRT)")(
      tokenizer.getTokenAtPosition("crt]", 3).toString)
    expectResult("Token(crt,Ident,CRT)")(
      tokenizer.getTokenAtPosition("crt", 0).toString)
    expectResult("Token(crt,Ident,CRT)")(
      tokenizer.getTokenAtPosition("crt", 3).toString)
  }
  test("Empty1") {
    val tokens = tokenize("")
    expectResult("")(tokens.mkString)
  }
  test("Empty2") {
    val tokens = tokenize("\n")
    expectResult("")(tokens.mkString)
  }
  test("underscore") {
    val tokens = tokenize("_")
    expectResult("Token(_,Ident,_)")(tokens.mkString)
  }
  test("ListOfArrays") {
    val tokens = tokenize("[{{array: 0}} {{array: 1}}]")
    expectResult("Token([,OpenBracket,null)" +
                 "Token({{array: 0}},Literal,{{array: 0}})" +
                 "Token({{array: 1}},Literal,{{array: 1}})" +
                 "Token(],CloseBracket,null)")(
      tokens.mkString)
    expectResult(1)(tokens(1).startPos)
    expectResult(13)(tokens(1).endPos)
    expectResult(14)(tokens(2).startPos)
    expectResult(26)(tokens(2).endPos)
  }

  test("ArrayOfArrays") {
    val tokens = tokenize("{{array: 2: {{array: 0}} {{array: 1}}}}")
    expectResult("Token({{array: 2: {{array: 0}} {{array: 1}}}},Literal,{{array: 2: {{array: 0}} {{array: 1}}}})")(
      tokens.mkString)
  }

  test("UnclosedExtensionLiteral1") {
    val tokens = tokenizeRobustly("{{array: 1: ")
    expectResult("Token(,Bad,End of file reached unexpectedly)")(
      tokens.mkString)
  }
  test("UnclosedExtensionLiteral2") {
    val tokens = tokenizeRobustly("{{")
    expectResult("Token(,Bad,End of file reached unexpectedly)")(
      tokens.mkString)
  }
  test("UnclosedExtensionLiteral3") {
    val tokens = tokenizeRobustly("{{\n")
    expectResult("Token(,Bad,End of line reached unexpectedly)")(
      tokens.mkString)
  }

  test("carriageReturnsAreWhitespace") {
    val tokens = tokenize("a\rb")
    expectResult("Token(a,Ident,A)" + "Token(b,Ident,B)")(
      tokens.mkString)
  }

  /// Unicode
  test("unicode") {
    val o ="\u00F6"  // lower case o with umlaut
    val tokens = tokenize(o)
    expectResult("Token(" + o + ",Ident," + o.toUpperCase + ")")(
      tokens.mkString)
  }
  test("TokenizeBadCharactersInIdent") {
    // 216C is a Unicode character I chose pretty much at random.  it's a Roman numeral
    // for fifty, and *looks* just like an L, but is not a letter according to Unicode.
    val tokens = tokenizeRobustly("foo\u216Cbar")
    expectResult(3)(firstBadToken(tokens).get.startPos)
    expectResult(4)(firstBadToken(tokens).get.endPos)
    expectResult("This non-standard character is not allowed.")(
      firstBadToken(tokens).get.value)
  }
  test("TokenizeOddCharactersInString") {
    val tokens = tokenize("\"foo\u216C\"")
    val expected = "Token(\"foo\u216C\",Constant,foo\u216C)"
    expectResult(expected)(tokens.mkString)
  }

}
