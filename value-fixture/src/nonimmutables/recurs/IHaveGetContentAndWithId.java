package nonimmutables.recurs;

public interface IHaveGetContentAndWithId<T extends MyContent, A extends MyBase> extends MyBase, IHaveWithIdMethod<A> {

  T getContent();
}
