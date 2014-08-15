exports.newLongFieldsAlignment = function() {
  let fieldSize = 63
  let toHex = function(x) '0x' + java.lang.Integer.toHexString(x)
  let toBin= function(x) '0b' + java.lang.Integer.toBinaryString(x)
  let computeMask = function(i) let(s = i - 1) s == 0 ? 1 : ((1 << s) | computeMask(s))
  let computeBitsFor = function(value) 32 - java.lang.Integer.numberOfLeadingZeros(value)

  return {
    index : -1
  , offset: 0
  , min : 0
  , max : 0
  , size : 0
  , mask : '0x0'
  , bin : '0b0'
  , nextAttribute : function(type, min, max) {
      this.type = type
      if (type == 'boolean') {
        min = 0, max = 1
      }
      this.min = min
      this.max = max
      let previousSize = this.size 
      this.size = computeBitsFor(max - min)
      this.mask = toHex(computeMask(this.size))
      this.bin = toBin(computeMask(this.size))
      if (this.index < 0) {
        this.index = 0
      }
      if (previousSize + this.offset + this.size > fieldSize) {
        this.index++
        this.offset = 0
        return true
      } else {
        this.offset += previousSize
        return (previousSize == 0)
      }
    }
  }
}

exports.wrappingImmutableCollection = function(attribute, expression) {
  let v = attribute
    , t = attribute.type
  
  let parensExpression = '(' + expression + ')' 
  
  if (v.generateEnumSet)
    return 'com.google.common.collect.Sets.immutableEnumSet' + parensExpression

  if (v.generateOrdinalValueSet)
    return 'org.immutables.common.collect.ImmutableOrdinalSet.copyOf' + parensExpression
    
  if (v.collectionType)
    return 'com.google.common.collect.Immutable' + v.rawCollectionType + '.copyOf' + parensExpression
    
  if (v.mapType) {
    if (v.generateEnumMap)
      return 'com.google.common.collect.Maps.immutableEnumMap' + parensExpression
    
    return 'com.google.common.collect.ImmutableMap.copyOf' + parensExpression
  }
  
  throw 'Unsupported attribute type: ' + v.type
}

exports.compareFields = function(attribute) {
  let equalsSimple = function(n) n + ' == another.' + n
  let equalsMethod = function(n) n + '.equals(another.' + n + ')'
  let equalsFloatBits = function(n) 'Float.floatToIntBits(' + n + ') == Float.floatToIntBits(another.' + n + ')'
  let equalsDoubleBits = function(n) 'Double.doubleToLongBits(' + n + ') == Double.doubleToLongBits(another.' + n + ')'

  return let (n = attribute.name, t = attribute.type)
      attribute.primitive ? (
          t === 'float' ? equalsFloatBits(n) :
          t === 'double' ? equalsDoubleBits(n) : equalsSimple(n)
          ) : equalsMethod(n)
}
