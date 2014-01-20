let template = require('org/immutables/generate/template/template.js')

exports.main = function (type, javaSinkFactory) {

  javaSinkFactory.sinkFor(type.packageName + '.Immutable' + type.name).write(
      template.require('org/immutables/generate/template/immutable.tjs').generateImmutableType(type))
      

  if (type.generateModifiable) {
    javaSinkFactory.sinkFor(type.packageName + '.Modifiable' + type.name).write(
        template.require('org/immutables/generate/template/modifiable.tjs').generateModifiableType(type))    
  }
  
  if (type.helperAttributes.length > 0) {
    javaSinkFactory.sinkFor(type.packageName + '.' + type.name + 'Functions').write(
        template.require('org/immutables/generate/template/functions.tjs').generateFunctions(type))
  } 
  
  if (type.generateMarshaled) {
    let marshalerTemplate = template.require('org/immutables/generate/template/marshaler.tjs')
    
    javaSinkFactory.sinkFor(type.packageName + '.' + type.name + "Marshaler").write(
        marshalerTemplate.generateMarshaler(type))
    
    javaSinkFactory.sinkFor(type.packageName + '.Internal' + type.name + "Marshaling").write(
        marshalerTemplate.generateMarshaling(type))
  }
  
  if (type.generateDocument) {
    let documentTemplates = template.require('org/immutables/generate/template/repository.tjs')
    
    javaSinkFactory.sinkFor(type.packageName + '.' + type.name + 'Repository').write(
        documentTemplates.generateRepository(type))
  }
}