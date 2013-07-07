let fieldSize = 63
let toHex = function(x) '0x' + java.lang.Integer.toHexString(x)
let toBin= function(x) '0b' + java.lang.Integer.toBinaryString(x)
let computeMask = function(i) let(s = i - 1) s == 0 ? 1 : ((1 << s) | computeMask(s))
let computeBitsFor = function(value) 32 - java.lang.Integer.numberOfLeadingZeros(value)

exports.newLongFieldsAlignment = function() {
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
