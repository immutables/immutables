{
  let self = this;  
  
  self.copyAsArray = function (arr) Array.prototype.slice.call(arr)

  self.println = function () {
    java.lang.System.out.println(self.copyAsArray(arguments).join(''))
    return self.println
  }
  
  self.hashOf = Packages.org.immutables.generate.internal.javascript.Predefined.hashOf
  
  self.assert = function (condition) {
    if (!condition) throw new java.lang.AssertionError()
  }
  
  self.readLinesForModule = function (moduleId) __moduleSourceProvider__.apply(moduleId)
  
  String.prototype.toUpperFirst = function () this.charAt(0).toUpperCase() + this.substring(1)

  String.prototype.toLowerFirst = function () this.charAt(0).toLowerCase() + this.substring(1)
}