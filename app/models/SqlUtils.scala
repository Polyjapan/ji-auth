package models

import java.sql.Connection

import anorm.Macro.ColumnNaming
import anorm.SqlParser.scalar
import anorm.{BatchSql, NamedParameter, ParameterValue, SQL, ToParameterList}

object SqlUtils {
  /**
   * Inserts one item in the given table and returns its id
   *
   * @param table the table in which the item shall be inserted
   * @param item  the item that shall be inserted
   * @return the id of the inserted item
   */
  def insertOne[T](table: String, item: T, upsert: Boolean = false)(implicit parameterList: ToParameterList[T], conn: Connection): Int = {
    val params: Seq[NamedParameter] = parameterList(item);
    val names: List[String] = params.map(_.name).toList
    val colNames = names.map(ColumnNaming.SnakeCase) mkString ", "
    val placeholders = names.map { n => s"{$n}" } mkString ", "
    val updateData = names.map(k => ColumnNaming.SnakeCase(k) + " = {" + k + "}").mkString(", ")


    SQL("INSERT INTO " + table + "(" + colNames + ") VALUES (" + placeholders + ")" + (if (upsert) " ON DUPLICATE KEY UPDATE " + updateData else ""))
      .bind(item)
      .executeInsert(scalar[Int].singleOpt)
      .getOrElse(0)
  }

  /**
   * Updates all fields of one item in the given table
   *
   * @param table   the table in which the item shall be replaced
   * @param item    the item that shall be added
   * @param idField the name of the ID field in the ADT
   * @return the id of the inserted item
   */
  def replaceOne[T](table: String, item: T, idField: String)(implicit parameterList: ToParameterList[T], conn: Connection): Int = {
    val params: Map[String, ParameterValue] = parameterList(item).map(np => np.tupled).toMap

    val queryData = params.keys.map(k => ColumnNaming.SnakeCase(k) + " = {" + k + "}").mkString(", ")

    SQL("UPDATE " + table + " SET " + queryData + " WHERE " + ColumnNaming.SnakeCase(idField) + " = {" + idField + "}")
      .bind(item)
      .executeUpdate()
  }

  /**
   * Inserts items in the given table
   *
   * @param table the table in which the items shall be inserted
   * @param items the items that shall be inserted
   */
  def insertMultiple[T](table: String, items: Iterable[T])(implicit parameterList: ToParameterList[T], conn: Connection) = {
    val params: Seq[NamedParameter] = parameterList(items.head);
    val names: List[String] = params.map(_.name).toList
    val colNames = names.map(ColumnNaming.SnakeCase) mkString ", "
    val placeholders = names.map { n => s"{$n}" } mkString ", "

    BatchSql("INSERT INTO " + table + "(" + colNames + ") VALUES (" + placeholders + ")", params, items.tail.map(parameterList).toSeq: _*)
      .execute()
  }
}
