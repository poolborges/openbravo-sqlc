//Sqlc generated V1.O00-1
package com.poolborges.example.sqlc1;

import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;

import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.data.SqlcException;

/**
 * Example for a sqlc generated class
 */
public class SakilaActor implements FieldProvider {

    static Logger log4j = Logger.getLogger(SakilaActor.class);
    private String InitRecordNumber = "0";
    public String actorId;
    public String firstName;
    public String lastName;
    public String lastUpdate;

    public String getInitRecordNumber() {
        return InitRecordNumber;
    }

    public String getField(String fieldName) {
        if (fieldName.equalsIgnoreCase("actor_id") || fieldName.equals("actorId")) {
            return actorId;
        } else if (fieldName.equalsIgnoreCase("first_name") || fieldName.equals("firstName")) {
            return firstName;
        } else if (fieldName.equalsIgnoreCase("last_name") || fieldName.equals("lastName")) {
            return lastName;
        } else if (fieldName.equalsIgnoreCase("last_update") || fieldName.equals("lastUpdate")) {
            return lastUpdate;
        } else {
            log4j.debug("Field does not exist: " + fieldName);
            return null;
        }
    }

    public static SakilaActor[] selectAllActor(ConnectionProvider connectionProvider) throws SqlcException {
        return selectAllActor(connectionProvider, 0, 0);
    }

    public static SakilaActor[] selectAllActor(ConnectionProvider connectionProvider, int firstRegister, int numberRegisters) throws SqlcException {
        String strSql = "";
        strSql = strSql
                + "            SELECT actor_id, first_name, last_name, last_update FROM actor";

        ResultSet result;
        Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
        PreparedStatement st = null;

        try {
            st = connectionProvider.getPreparedStatement(strSql);

            result = st.executeQuery();
            long countRecord = 0;
            long countRecordSkip = 1;
            boolean continueResult = true;
            while (countRecordSkip < firstRegister && continueResult) {
                continueResult = result.next();
                countRecordSkip++;
            }
            while (continueResult && result.next()) {
                countRecord++;
                SakilaActor objectSakilaActor = new SakilaActor();
                objectSakilaActor.actorId = UtilSql.getValue(result, "actor_id");
                objectSakilaActor.firstName = UtilSql.getValue(result, "first_name");
                objectSakilaActor.lastName = UtilSql.getValue(result, "last_name");
                objectSakilaActor.lastUpdate = UtilSql.getDateValue(result, "last_update", "null");
                objectSakilaActor.InitRecordNumber = Integer.toString(firstRegister);
                vector.addElement(objectSakilaActor);
                if (countRecord >= numberRegisters && numberRegisters != 0) {
                    continueResult = false;
                }
            }
            result.close();
        } catch (SQLException e) {
            log4j.error("SQL error in query: " + strSql + "Exception:" + e);
            throw new SqlcException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
        } catch (Exception ex) {
            log4j.error("Exception in query: " + strSql + "Exception:" + ex);
            throw new SqlcException("@CODE=@" + ex.getMessage());
        } finally {
            try {
                connectionProvider.releasePreparedStatement(st);
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        SakilaActor objectSakilaActor[] = new SakilaActor[vector.size()];
        vector.copyInto(objectSakilaActor);
        return (objectSakilaActor);
    }

    public static SakilaActor[] selectActorByMovieTitle(ConnectionProvider connectionProvider, String movieTitle) throws SqlcException {
        return selectActorByMovieTitle(connectionProvider, movieTitle, 0, 0);
    }

    public static SakilaActor[] selectActorByMovieTitle(ConnectionProvider connectionProvider, String movieTitle, int firstRegister, int numberRegisters) throws SqlcException {
        String strSql = "";
        strSql = strSql
                + "            SELECT actor.* FROM actor"
                + "            INNER JOIN film_actor ON film_actor.actor_id = actor.actor_id"
                + "            INNER JOIN film ON film.film_id = film_actor.film_id"
                + "            WHERE film.title = ?;";

        ResultSet result;
        Vector<java.lang.Object> vector = new Vector<java.lang.Object>(0);
        PreparedStatement st = null;

        int iParameter = 0;
        try {
            st = connectionProvider.getPreparedStatement(strSql);
            iParameter++;
            UtilSql.setValue(st, iParameter, 12, null, movieTitle);

            result = st.executeQuery();
            long countRecord = 0;
            long countRecordSkip = 1;
            boolean continueResult = true;
            while (countRecordSkip < firstRegister && continueResult) {
                continueResult = result.next();
                countRecordSkip++;
            }
            while (continueResult && result.next()) {
                countRecord++;
                SakilaActor objectSakilaActor = new SakilaActor();
                objectSakilaActor.actorId = UtilSql.getValue(result, "actor_id");
                objectSakilaActor.firstName = UtilSql.getValue(result, "first_name");
                objectSakilaActor.lastName = UtilSql.getValue(result, "last_name");
                objectSakilaActor.lastUpdate = UtilSql.getDateValue(result, "last_update", "null");
                objectSakilaActor.InitRecordNumber = Integer.toString(firstRegister);
                vector.addElement(objectSakilaActor);
                if (countRecord >= numberRegisters && numberRegisters != 0) {
                    continueResult = false;
                }
            }
            result.close();
        } catch (SQLException e) {
            log4j.error("SQL error in query: " + strSql + "Exception:" + e);
            throw new SqlcException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
        } catch (Exception ex) {
            log4j.error("Exception in query: " + strSql + "Exception:" + ex);
            throw new SqlcException("@CODE=@" + ex.getMessage());
        } finally {
            try {
                connectionProvider.releasePreparedStatement(st);
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        SakilaActor objectSakilaActor[] = new SakilaActor[vector.size()];
        vector.copyInto(objectSakilaActor);
        return (objectSakilaActor);
    }
}
