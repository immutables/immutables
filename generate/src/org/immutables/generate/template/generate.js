let templating = require('org/immutables/generate/template/template.js')

exports.main = function (type, javaSinkFactory) {
  let filer = packageFiler()

  let immutableTemplate = template('immutable')

  filer(type.defName)(type.emptyNesting
      ? immutableTemplate.generateImmutableNestingType(type)
      : immutableTemplate.generateImmutableSimpleType(type))
  
  if (type.hasNestedChildren) {
    type.nestedChildren.forEach(generateOtherArtifacts)
  }
  
  if (!type.emptyNesting) generateOtherArtifacts(type)

  function generateOtherArtifacts(type) {
    if (type.generateMarshaled) {
      let marshalerTemplate = template('marshaler')
      
      filer(type.simpleName, 'Marshaler')(marshalerTemplate.generateMarshaler(type))
      filer('_Marshaling_', type.simpleName)(marshalerTemplate.generateMarshaling(type))
    }

    if (type.generateModifiable) {
      filer('Modifiable', type.simpleName)(template('modifiable').generateModifiableType(type))    
    }
    
    // For now disable generation of additional artifacts to nested classes
    if (type.name != type.simpleName) return
  
    if (type.helperAttributes.length > 0) {
      filer(type.name, 'Functions')(template('functions').generateFunctions(type))
    }
    
    if (type.generateDocument) {
      filer(type.name,'Repository')(template('repository').generateRepository(type))
    }
  }
  
  function template(name) {
    return templating.require('org/immutables/generate/template/' + name + '.tjs')
  }
  
  function packageFiler() {
    return function() {
      let packagePrefix = (type.packageName ? type.packageName + '.' : ''),
          className = [].slice.call(arguments, 0).filter(Boolean).join(''),
          sink = javaSinkFactory.sinkFor(packagePrefix + className)
          
      return function(sourceCode) {
        sink.write(sourceCode)
      }
    }
  }
}