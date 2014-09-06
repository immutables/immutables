let template = require('org/immutables/generate/template/template.js')

exports.main = function (type, javaSinkFactory) {

  let immutableTjs = template.require('org/immutables/generate/template/immutable.tjs'),
      immutableSink = javaSinkFactory.sinkFor(type.packageName + '.' + type.defName)

   immutableSink.write(type.emptyNesting
      ? immutableTjs.generateImmutableNestingType(type)
      : immutableTjs.generateImmutableSimpleType(type))
  
  if (type.hasNestedChildren) {
    type.nestedChildren.forEach(generateOtherArtifacts)
  }
  
  if (!type.emptyNesting) generateOtherArtifacts(type)

  function generateOtherArtifacts(type) {
    if (type.generateMarshaled) {
      let marshalerTemplate = template.require('org/immutables/generate/template/marshaler.tjs')
      
      javaSinkFactory.sinkFor(type.packageName + '.' + type.simpleName + "Marshaler").write(
          marshalerTemplate.generateMarshaler(type))
      
      javaSinkFactory.sinkFor(type.packageName + '.Internal' + type.simpleName + "Marshaling").write(
          marshalerTemplate.generateMarshaling(type))
    }
    
    // For now disable generation of additional artifacts to nested classes (with parent annotated @GenerateNested)
    if (type.hasNestingParent) return
  
    if (type.generateModifiable) {
      javaSinkFactory.sinkFor(type.packageName + '.Modifiable' + type.name).write(
          template.require('org/immutables/generate/template/modifiable.tjs').generateModifiableType(type))    
    }
    
    if (type.helperAttributes.length > 0) {
      javaSinkFactory.sinkFor(type.packageName + '.' + type.name + 'Functions').write(
          template.require('org/immutables/generate/template/functions.tjs').generateFunctions(type))
    }
    
    if (type.generateDocument) {
      let documentTemplates = template.require('org/immutables/generate/template/repository.tjs')
      
      javaSinkFactory.sinkFor(type.packageName + '.' + type.name + 'Repository').write(
          documentTemplates.generateRepository(type))
    }
  }
}