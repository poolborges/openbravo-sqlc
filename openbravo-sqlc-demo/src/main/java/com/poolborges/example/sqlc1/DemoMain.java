/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poolborges.example.sqlc1;

import java.sql.DriverManager;
import org.openbravo.data.SqlcException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.exception.PoolNotFoundException;

/**
 *
 * @author pauloborges
 */
public class DemoMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SqlcException, PoolNotFoundException {
        
        ConnectionProvider connectionProvider = new ConnectionProviderImpl("./workdir/connection.properties");
        SakilaActor[] sa = SakilaActor.selectAllActor(connectionProvider);
        
        for (int i = 0; i < sa.length; i++)
            DriverManager.println(sa[i].firstName);
    }
    
}
