import Surface.Term

class Parse private constructor(
  private val text: String,
) {
  private var cursor: Int = 0

  private fun parse(): Term {
    val term = parseTerm()
    skipWhitespace()
    check(!canRead())
    return term
  }

  private fun parseTerm(): Term {
    val terms = mutableListOf<Term>()
    skipWhitespace()
    while (canRead() && when (peek()) {
        ')', '→', ';', ':' -> false
        else               -> true
      }
    ) {
      terms += parseTerm0()
      skipWhitespace()
    }
    return terms.reduce { acc, term ->
      Term.App(acc, term)
    }
  }

  private fun parseTerm0(): Term {
    skipWhitespace()
    return when (peek()) {
      'Π'  -> {
        skip()
        when (peek()) {
          '('  -> {
            skip()
            val name = parseWord()
            expect(':')
            val param = parseTerm()
            expect(')')
            expect('→')
            val result = parseTerm()
            Term.Func(name, param, result)
          }
          else -> {
            val param = parseTerm()
            expect('→')
            val result = parseTerm()
            Term.Func(null, param, result)
          }
        }
      }

      'λ'  -> {
        skip()
        val name = parseWord().takeUnless { it == "_" }
        expect('.')
        val body = parseTerm()
        Term.FuncOf(name, body)
      }

      '('  -> {
        skip()
        when (peek()) {
          ')'  -> {
            skip()
            Term.UnitOf
          }

          else -> {
            val term = parseTerm()
            when (peek()) {
              ':'  -> {
                skip()
                val type = parseTerm()
                expect(')')
                Term.Anno(term, type)
              }

              ')'  -> {
                skip()
                term
              }

              else -> {
                error("unexpected '${peek()}'")
              }
            }
          }
        }
      }

      else -> {
        when (val word = parseWord()) {
          "Type" -> {
            Term.Type
          }

          "Unit" -> {
            Term.Unit
          }

          "let"  -> {
            val name = parseWord().takeUnless { it == "_" }
            expect('=')
            val init = parseTerm()
            expect(';')
            val body = parseTerm()
            Term.Let(name, init, body)
          }

          else   -> {
            Term.Var(word)
          }
        }
      }
    }
  }

  private fun parseWord(): String {
    skipWhitespace()
    val start = cursor
    while (canRead() && peek().isWordLetter()) {
      skip()
    }
    check(start < cursor)
    return text.substring(start, cursor)
  }

  private fun Char.isWordLetter(): Boolean {
    return isLetterOrDigit() || this == '_'
  }

  private fun expect(expected: Char) {
    skipWhitespace()
    return if (canRead() && peek() == expected) {
      skip()
    } else {
      error("expected '$expected', got '${peek()}'")
    }
  }

  private fun skipWhitespace() {
    while (canRead() && peek().isWhitespace()) {
      skip()
    }
  }

  private fun skip() {
    ++cursor
  }

  private fun peek(): Char {
    return text[cursor]
  }

  private fun canRead(): Boolean {
    return cursor < text.length
  }

  companion object {
    operator fun invoke(
      text: String,
    ): Term {
      return Parse(text).parse()
    }
  }
}
