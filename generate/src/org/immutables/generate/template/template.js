let functionIdentifierRegex = /\s?([a-zA-Z0-9][a-zA-Z0-9\.]*)\s*(\(.*)?/,
    commentLineRegex = /\s*\[--.*/,
    statementLineRegex = /(\s*)(\[\/?)([a-z]*)([^\]]*)\]\s*/
  
let statementTransformations = {
  '[apply': function (whole, identifier, rest, whitespace)
      '__.apply(' + identifier + ', __templateLib__.copyArguments' + (rest || '') + ',' + JSON.stringify(whitespace) + ');',
  '[template': function (whole, identifier, rest)
      'let ' + identifier + ' = __templateExports__.' + identifier + ' = function ' + (rest || '()')
          + ' { let (__ = this.__context || __templateLib__.newContext()) {',
  '[/template': ';if (!this.__context) return __.result() }};',
  '[let': ['let', '{'],
  '[/let': '}',
  '[foreach': ['{let nextComma = "";for each', '{'],
  '[/foreach': ';nextComma=","}}',
  '[if': ['if', '{'],
  '[/if': '}',
  '[else': '} else {',
  '[elseif': ['} else if', '{'],
  '[requirejs': function (whole, identifier, rest) 'let ' + identifier + ' = require' + rest + ';',
  '[require': function (whole, identifier, rest) 'let ' + identifier + ' = __templateLib__.require' + rest + ';'
}

function applyTransformation(match, whitespace, rest, transformation) {
  if (transformation instanceof String || typeof transformation == 'string') {
    return whitespace + transformation
  }
  if (transformation instanceof Array) {
    return whitespace + transformation[0] + rest + transformation[1]
  }
  if (transformation instanceof Function || typeof transformation == 'function') {
    let [w, i, r] = functionIdentifierRegex.exec(rest) || (function () { throw match.toSource() }())
    return whitespace + transformation.apply(null, [w, i, r, whitespace])
  }

  throw new Error(match.toSource())
}

function transformStatementLine(match) {
  match = match || []
  let [, whitespace, symbol, statement, rest] = match
  let transformation = statementTransformations[symbol + statement]
  if (transformation) {
    return applyTransformation(match, whitespace, rest, transformation)
  }
  throw new Error(match.toSource())
}

let transformLiteralLine = function (l) l.replace('#', '')

function transformInterpolationLine(l) {
  let parts = l.split('`'),
      result = []

  for (let i = 0; i < parts.length; i++) {
    let p = parts[i]
    if (i % 2 == 0) {
      p = JSON.stringify(p)
    } else if (p.trim().length == 0) {
      p = '""';
    }
    result.push(p)
  }

  return '__.out(' + result.join(', ') + ');'
}

function transformTemplateSyntaxToJs(lines) {
  let resultLines = []

  for each(let l in lines) {
    let openBracketIndex = l.indexOf('['),
        literalStatementIndex = l.indexOf('#'),
        backtickInterpolationIndex = l.indexOf('`'),
        commentIndex = l.indexOf('[--')

    if (commentIndex >= 0 && commentLineRegex.test(l)) {
      resultLines.push('//' + l)
      continue
    }

    let statementLineMatch = openBracketIndex >= 0 ? (statementLineRegex.exec(l)) : null
    if (literalStatementIndex >= 0 && l.trim().charAt(0) == '#') {
      resultLines.push(transformLiteralLine(l))
    } else if (statementLineMatch && (backtickInterpolationIndex < 0 || openBracketIndex < backtickInterpolationIndex)) {
      resultLines.push(transformStatementLine(statementLineMatch))
    } else {
      resultLines.push(transformInterpolationLine(l))
    }
  }

  return resultLines
}

let templateLib = {
  newContext: function () {
    return {
      lib: this,
      resultLines: [],
      indent: '',
      apply: function (templateFunction, templateArguments, whitespace) {
        let a = templateArguments instanceof Array ? templateArguments : []
        let oldIndent = this.indent
        this.indent += whitespace
        templateFunction.apply({ __context:this }, a)
        this.indent = oldIndent
      },
      out: function () {
        this.resultLines.push(this.indent + Array.prototype.join.call(arguments, ''))
      },
      result: function () this.resultLines.join('\n')
    }
  },

  require: function (moduleId) requireTemplates(moduleId),

  copyArguments: function () Array.prototype.slice.call(arguments)
}

let templateCache = {}

function requireTemplates(moduleId) {
  let templateObject = templateCache[moduleId]
  if (!templateObject) {
    templateObject = {}

    let templatingLines = __moduleSourceProvider__.apply(moduleId)
      , lines = transformTemplateSyntaxToJs(templatingLines)
      , functionCode = lines.join('\n')

    let compiledFunction = function () {
      try {
        return new Function('__templateExports__', '__templateLib__', '__', functionCode)
      } catch (e) {
        throw 'Template Compile Error: ' + e + '\nTemplate line ' + e.lineNumber + ': ' + (templatingLines[e.lineNumber - 1] || '<EOF>')
      }
    }()
    let nop = function () {}
    compiledFunction(templateObject, templateLib, { out:nop, apply:nop, result:nop })
    templateCache[moduleId] = templateObject
  }

  return templateObject
}

exports.require = requireTemplates