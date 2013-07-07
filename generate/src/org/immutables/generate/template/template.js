let functionIdentifierRegex = /\s?([a-zA-Z0-9][a-zA-Z0-9\.]*)\s*(\(.*)?/
let commentLineRegex = /\s*\[--.*/
let statementLineRegex = /(\s*)(\[\/?)([a-z]*)([^\]]*)\]\s*/
  
function error(s) {
  throw s || new Error
}

let statementTransformations = {
  '[apply': function(whole, identifier, rest, whitespace)
      '__.apply(' + identifier + ', __templateLib__.copyArguments' + (rest || '') + ',' + JSON.stringify(whitespace) + ');',
  '[template': function(whole, identifier, rest)
      'var ' + identifier + ' = __templateExports__.' + identifier + ' = function ' + (rest || '()')
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

let applyTransformation = function(match, whitespace, rest, transformation) {
  if (transformation instanceof String || typeof transformation == 'string') {
    return whitespace + transformation
  }
  if (transformation instanceof Array) {
    return whitespace + transformation[0] + rest + transformation[1]
  }
  if (transformation instanceof Function || typeof transformation == 'function') {
    let [w, i, r] = functionIdentifierRegex.exec(rest) || (function() { throw match.toSource() }())
    return whitespace + transformation.apply(null, [w, i, r, whitespace])
  }

  throw match.toSource()
}

let transformStatementLine = function(match) {
  match = match || []
  let [, whitespace, symbol, statement, rest] = match
  let transformation = statementTransformations[symbol + statement]
  if (transformation) {
    return applyTransformation(match, whitespace, rest, transformation)
  }
  throw new Error(match.toSource())
}

let transformLiteralLine = function(l) l.replace('#', '')

let transformInterpolationLine = function(l) {
  let parts = l.split('`'),
      result = []

  for (let i = 0; i < parts.length; i++) {
    let p = parts[i]
    if (i % 2 == 0) {
      p = JSON.stringify(p)
    }
    result.push(p)
  }

  return '__.out(' + result.join(', ') + ');'
}

let transformTemplateSyntaxToJs = function(lines) {
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
    if (statementLineMatch && (backtickInterpolationIndex < 0 || openBracketIndex < backtickInterpolationIndex)) {
      resultLines.push(transformStatementLine(statementLineMatch))
    } else if (literalStatementIndex >= 0 && (backtickInterpolationIndex < 0 || literalStatementIndex < backtickInterpolationIndex)) {
      resultLines.push(transformLiteralLine(l))
    } else {
      resultLines.push(transformInterpolationLine(l))
    }
  }

  return resultLines
}

let templateLib = {
  newContext: function() {
    return {
      lib: this,
      resultLines: [],
      indent: '',
      apply: function(templateFunction, templateArguments, whitespace) {
        let a = templateArguments instanceof Array ? templateArguments : []
        let oldIndent = this.indent
        this.indent += whitespace
        templateFunction.apply({ __context:this }, a)
        this.indent = oldIndent
      },
      out: function() {
        this.resultLines.push(this.indent + copyAsArray(arguments).join(''))
      },
      result: function() this.resultLines.join('\n')
    }
  },

  require: function(moduleId) exports.require(moduleId),

  copyArguments: function() copyAsArray(arguments)
}

var templateCache = {}

let requireTemplates = function(moduleId) {
  let templateObject = templateCache[moduleId]
  if (!templateObject) {
    templateObject = {}
    let lines = transformTemplateSyntaxToJs(readLinesForModule(moduleId))
    let functionCode = lines.join('\n');
    let compiledFunction = function () {
      try {
        return new Function('__templateExports__', '__templateLib__', '__', functionCode)
      } catch (e) {
        throw 'Template Compile error: ' + e + '\nBody: ***\n' + functionCode + '\n***' 
      }
    }()
    let noOp = function() {}
    compiledFunction(templateObject, templateLib, { out:noOp, apply:noOp, result:noOp })
    templateCache[moduleId] = templateObject
  }

  return templateObject
}

exports.require = requireTemplates