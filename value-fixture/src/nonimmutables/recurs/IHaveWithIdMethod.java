package nonimmutables.recurs;

public interface IHaveWithIdMethod<T extends MyBase> {

  T withId(int id); // I that having getId in MyBase will generate this but I need an interface to
// use commonly on multiple class that inherit from MyBase
}
