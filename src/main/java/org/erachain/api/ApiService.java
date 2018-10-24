package org.erachain.api;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.IPAccessHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import org.erachain.settings.Settings;

public class ApiService {

    public Server server;

    public ApiService() {
        //CREATE CONFIG
        Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(CoreResource.class);
        s.add(SeedResource.class);
        s.add(PeersResource.class);
        s.add(TransactionsResource.class);
        s.add(TelegramsResource.class);
        s.add(BlocksResource.class);
        s.add(AddressesResource.class);
        s.add(WalletResource.class);
        s.add(R_SendResource.class);
        s.add(Rec_PaymentResource.class);
        s.add(NamesResource.class);
        s.add(NameSalesResource.class);
        s.add(PollsResource.class);
        s.add(ArbitraryTransactionsResource.class);
        s.add(NamePaymentResource.class);
        s.add(ATResource.class);
        s.add(BlogPostResource.class);
        s.add(BlogResource.class);
        //s.add(CalcFeeResource.class);
        s.add(NameStorageResource.class);
        //	s.add(Rec_MessageResource.class);
        s.add(Rec_Resource.class);
        s.add(Rec_HashesResource.class);
        s.add(AssetsResource.class);
        s.add(Rec_StatementResource.class);
        s.add(MultiPaymentResource.class);
        s.add(TradeResource.class);

        ResourceConfig config = new ResourceConfig(s);

        //CREATE CONTAINER
        ServletContainer container = new ServletContainer(config);

        //CREATE CONTEXT
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(container), "/*");

        //CREATE WHITELIST
        IPAccessHandler accessHandler = new IPAccessHandler();
        accessHandler.setWhite(Settings.getInstance().getRpcAllowed());
        accessHandler.setHandler(context);

        //CREATE RPC SERVER
        this.server = new Server(Settings.getInstance().getRpcPort());
        this.server.setHandler(accessHandler);
    }

    public void start() {
        try {
            //START RPC
            server.start();
        } catch (Exception e) {
            //FAILED TO START RPC
        }
    }

    public void stop() {
        try {
            //STOP RPC
            server.stop();
        } catch (Exception e) {
            //FAILED TO STOP RPC
        }
    }
}