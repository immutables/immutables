{
  String.prototype.toUpperFirst = function () this.charAt(0).toUpperCase() + this.substring(1)

  String.prototype.toLowerFirst = function () this.charAt(0).toLowerCase() + this.substring(1)
}