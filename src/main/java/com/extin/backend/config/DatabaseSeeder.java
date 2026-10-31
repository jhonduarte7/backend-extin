package com.extin.backend.config;

import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.CustomerEquipment;
import com.extin.backend.models.entities.Invoice;
import com.extin.backend.models.entities.InvoiceDetail;
import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.entities.QuoteDetail;
import com.extin.backend.models.entities.User;
import com.extin.backend.models.enums.InvoiceStatus;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.models.enums.QuoteStatus;
import com.extin.backend.models.enums.Role;
import com.extin.backend.repositories.CatalogItemRepository;
import com.extin.backend.repositories.CustomerEquipmentRepository;
import com.extin.backend.repositories.CustomerRepository;
import com.extin.backend.repositories.InvoiceRepository;
import com.extin.backend.repositories.QuoteRepository;
import com.extin.backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseSeeder runs upon application startup.
 * Automatically seeds:
 * 1. 50 realistic mock customers (Venezuelan RIF & Brazilian CNPJ).
 * 2. Customer equipment (both active and expired for maintenance alert queries).
 * 3. 20 assorted catalog products and maintenance services.
 * 4. Realistic quotes in PENDING and APPROVED states (ready to test invoicing in Postman).
 * 5. Realistic paid invoices in VES, BRL, and USD with immutable line items and payment references.
 */
@Component
@org.springframework.core.annotation.Order(1)
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CustomerRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final CustomerEquipmentRepository customerEquipmentRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.seed.admin-password:password123}")
    private String adminPassword;

    @Value("${app.security.seed.user-password:password123}")
    private String userPassword;

    public DatabaseSeeder(CustomerRepository customerRepository,
                          CatalogItemRepository catalogItemRepository,
                          CustomerEquipmentRepository customerEquipmentRepository,
                          QuoteRepository quoteRepository,
                          InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.customerEquipmentRepository = customerEquipmentRepository;
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long userCount = userRepository.count();
        long customerCount = customerRepository.count();
        long catalogCount = catalogItemRepository.count();
        long quoteCount = quoteRepository.count();

        // 1. Global check: If all core tables already have data, skip seeding entirely
        if (userCount > 0 && customerCount > 0 && catalogCount > 0 && quoteCount > 0) {
            log.info("DatabaseSeeder: Tables already populated (Users: {}, Customers: {}, Catalog: {}, Quotes: {}). Skipping all seed operations to protect database integrity.",
                    userCount, customerCount, catalogCount, quoteCount);
            syncAdminPassword();
            return;
        }

        // 2. Granular checks: Only insert if the respective table is completely empty (count == 0)
        if (userCount == 0) {
            log.info("Users table is empty. Seeding default ADMIN and USER accounts...");
            seedUsers();
        } else {
            log.info("Database already contains users ({} records). Skipping user seeding.", userCount);
            syncAdminPassword();
        }

        if (customerCount == 0) {
            log.info("Customers table is empty. Starting customer seeding...");
            seedCustomers();
        } else {
            log.info("Database already contains customer data ({} records). Skipping customer seeding.", customerCount);
        }

        if (catalogCount == 0) {
            log.info("Catalog table is empty. Starting catalog seeding...");
            seedCatalog();
        } else {
            log.info("Database already contains catalog data ({} records). Skipping catalog seeding.", catalogCount);
            System.out.println("Datos iniciales ya existen. Saltando inicialización.");
        }

        if (quoteCount == 0 && invoiceRepository.count() == 0) {
            log.info("Starting quote and invoice seeding for API and Postman testing...");
            seedQuotesAndInvoices();
        } else {
            log.info("Database already contains quotes/invoices. Skipping quote and invoice seeding.");
        }

        log.info("DatabaseSeeder initialization complete.");
    }

    private void seedUsers() {
        User adminUser = new User(
                "admin",
                passwordEncoder.encode(adminPassword),
                "admin@extin.com",
                Role.ADMIN
        );
        User standardUser = new User(
                "user",
                passwordEncoder.encode(userPassword),
                "user@extin.com",
                Role.USER
        );
        userRepository.saveAll(List.of(adminUser, standardUser));
        log.info("Seeded initial users: admin (Role.ADMIN), user (Role.USER).");
    }

    private void syncAdminPassword() {
        userRepository.findByUsername("admin").ifPresent(admin -> {
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                log.info("Updating admin user password in database to match configured APP_ADMIN_PASSWORD...");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                userRepository.save(admin);
                log.info("Admin user password synchronized successfully.");
            }
        });
    }

    private void seedCustomers() {
        log.info("Seeding 50 realistic mock customers (Venezuelan RIF & Brazilian CNPJ)...");

        // 25 Venezuelan Companies
        String[][] venefastCustomers = {
                {"J-12345678-9", "Inversiones El Sol C.A.", "Av. Francisco de Miranda, Torre Delta, Piso 4, Caracas", "+58 212-951-1234", "contacto@elsol.com"},
                {"J-20456789-1", "Manufacturas Metalúrgicas del Centro C.A.", "Zona Industrial San Vicente, Maracay, Aragua", "+58 243-551-8765", "ventas@metalcentro.ve"},
                {"J-31567890-2", "Distribuidora de Alimentos Los Andes S.A.", "Av. Las Américas, Edif. Los Pinos, Mérida", "+58 274-262-4321", "logistica@losandes.com"},
                {"J-40678901-3", "Constructora y Pavimentos Horizonte C.A.", "Calle 72 con Av. 15, Maracaibo, Zulia", "+58 261-798-1122", "proyectos@horizonteca.com"},
                {"J-50789012-4", "Farmacias Unidas de Venezuela S.A.", "Av. Bolívar Norte, Centro Comercial Camoruco, Valencia", "+58 241-825-9988", "seguridad@farmaciasunidas.ve"},
                {"J-60890123-5", "Clínica y Especialidades Santa Sofía C.A.", "Av. Principal del Cafetal, Caracas", "+58 212-985-3344", "mantenimiento@clinicasantasofia.com"},
                {"J-70901234-6", "Supermercados Plaza Mayor C.A.", "Av. Intercomunal Jorge Rodríguez, Lechería, Anzoátegui", "+58 281-286-5544", "administracion@plazamayor.com"},
                {"J-80012345-7", "Transportes y Encomiendas Guayana C.A.", "Zona Industrial Unare II, Puerto Ordaz, Bolívar", "+58 286-952-7700", "operaciones@transguayana.com"},
                {"J-90123456-8", "Agropecuaria y Ganadería Valle Verde C.A.", "Carretera Nacional Vía Calabozo, Guárico", "+58 246-871-2300", "info@valleverde.com"},
                {"J-10234567-0", "Hotel & Centro de Convenciones Bella Vista C.A.", "Calle Velásquez con Av. 4 de Mayo, Porlamar, Nueva Esparta", "+58 295-261-7222", "seguridad@hotelbellavista.ve"},
                {"J-21345678-1", "Petroquímica e Industrial del Zulia S.A.", "Av. 4 Bella Vista, Edif. Banvenez, Maracaibo", "+58 261-791-3400", "hse@petrozulia.com"},
                {"J-32456789-2", "Servicios Petroleros y Gasíferos Oriente C.A.", "Av. Municipal, Edif. Torre Oriente, Puerto La Cruz", "+58 281-267-9000", "gerencia@servioriente.com"},
                {"J-43567890-3", "Textiles y Confecciones La Guayanesa C.A.", "Paseo Caroní, Centro Empresarial Alta Vista, Puerto Ordaz", "+58 286-961-4500", "planta@laguayanesa.com"},
                {"J-54678901-4", "Panificadora Industrial El Buen Pan C.A.", "Av. Fuerzas Armadas, Esq. San Luis, Caracas", "+58 212-564-8899", "produccion@elbuenpan.com"},
                {"J-65789012-5", "Frigorífico y Almacenes FrigoCaracas C.A.", "Av. San Martín, Edif. Frigorífico, Caracas", "+58 212-461-2211", "calidad@frigocaracas.ve"},
                {"J-76890123-6", "Laboratorios Farmacéuticos Sanitas C.A.", "Urb. Los Ruices, Calle Milán, Caracas", "+58 212-239-6677", "seguridad.industrial@sanitaslab.com"},
                {"J-87901234-7", "Corporación Automotriz Central C.A.", "Zona Industrial El Recreo, Valencia, Carabobo", "+58 241-871-5500", "taller@autocentral.com"},
                {"J-98012345-8", "Embotelladora y Refrescos Aragüeña S.A.", "Av. Anton Phillips, Zona Industrial San Vicente, Maracay", "+58 243-553-9090", "ambiente@araguena.com"},
                {"V-14567890-1", "Comercializadora Hermanos Gómez", "Av. Urdaneta, Esq. Veroes, Local 12, Caracas", "+58 212-561-3421", "hermanosgomez@gmail.com"},
                {"V-17890123-2", "Taller Mecánico y Frenos El Tigre", "Av. Francisco de Miranda, El Tigre, Anzoátegui", "+58 283-241-6789", "frenoseltigre@gmail.com"},
                {"V-19012345-3", "Restaurante El Rincón Criollo", "Pueblo de El Hatillo, Calle Bolívar, Miranda", "+58 212-963-4455", "rinconcriollo@gmail.com"},
                {"G-20019283-4", "Gobernación y Bomberos del Estado Miranda", "Av. Independencia, Los Teques, Miranda", "+58 212-321-4567", "bomberos@miranda.gob.ve"},
                {"G-20038475-5", "Instituto Autónomo de Protección Civil Aragua", "Av. Las Delicias, Maracay, Aragua", "+58 243-242-1234", "prevencion@aragua.gob.ve"},
                {"J-11223344-5", "Inversiones Tecnológicas NetCom C.A.", "Av. Río de Janeiro, Chuao, Caracas", "+58 212-993-8877", "admin@netcom.com.ve"},
                {"J-55667788-9", "Centro Médico Quirúrgico Los Cedros C.A.", "Av. Libertador, Edif. Cedros Medical, Caracas", "+58 212-793-1122", "mantenimiento@loscedros.ve"}
        };

        // 25 Brazilian Companies
        String[][] brazilCustomers = {
                {"12.345.678/0001-90", "Indústria Metalúrgica Paulistana Ltda", "Av. Paulista, 1500, Bela Vista, São Paulo - SP", "+55 11 3254-8900", "contato@metalurgicapaulistana.com.br"},
                {"23.456.789/0001-01", "Logística e Transportes Rio Branco S.A.", "Rodovia Anhanguera, Km 18, Lapa, São Paulo - SP", "+55 11 3838-4400", "operacoes@logriobranco.com.br"},
                {"34.567.890/0001-12", "Supermercados Estrela do Sul Ltda", "Rua das Laranjeiras, 340, Laranjeiras, Rio de Janeiro - RJ", "+55 21 2558-1200", "adm@estreladosul.com.br"},
                {"45.678.901/0001-23", "Hospital e Maternidade São Lucas S.A.", "Av. Ipiranga, 6681, Partenon, Porto Alegre - RS", "+55 51 3320-3000", "seguranca@hospitalsaolucas.med.br"},
                {"56.789.012/0001-34", "Tecnologia e Segurança Campinas Ltda", "Rua Barão de Jaguara, 1120, Centro, Campinas - SP", "+55 19 3231-7788", "contato@teccampinas.com.br"},
                {"67.890.123/0001-45", "Comércio de Alimentos Brasil Central Ltda", "Av. T-63, 1200, Setor Bueno, Goiânia - GO", "+55 62 3281-9900", "vendas@brasilcentralalimentos.com.br"},
                {"78.901.234/0001-56", "Construtora Horizonte Verde S.A.", "Rua da Bahia, 1800, Lourdes, Belo Horizonte - MG", "+55 31 3274-5500", "obras@horizonteverde.com.br"},
                {"89.012.345/0001-67", "Distribuidora FarmaSul Ltda", "Rua Marechal Deodoro, 500, Centro, Curitiba - PR", "+55 41 3322-1100", "farmasul@farmasulpr.com.br"},
                {"90.123.456/0001-78", "Auto Peças Bandeirantes S.A.", "Av. Otacílio Tomanik, 780, Butantã, São Paulo - SP", "+55 11 3768-4500", "atendimento@pecasbandeirantes.com.br"},
                {"01.234.567/0001-89", "Química Industrial do Nordeste Ltda", "Polo Petroquímico de Camaçari, Camaçari - BA", "+55 71 3634-8000", "hse@quimicanordeste.ind.br"},
                {"11.222.333/0001-99", "Rede Hoteleira Mar Atlântico S.A.", "Av. Beira Mar, 2100, Meireles, Fortaleza - CE", "+55 85 3466-2000", "hotel@maratlantico.com.br"},
                {"22.333.444/0001-88", "Armazéns Gerais Santos Portuária Ltda", "Av. Cais do Porto, s/n, Macuco, Santos - SP", "+55 13 3224-7000", "porto@santosportuaria.com.br"},
                {"33.444.555/0001-77", "Cerâmica e Revestimentos Santa Catarina S.A.", "Rodovia SC-445, Km 5, Criciúma - SC", "+55 48 3461-9000", "contato@ceramicasantacatarina.com.br"},
                {"44.555.666/0001-66", "Agroflorestal Rio Doce Ltda", "Rua Afonso Pena, 920, Centro, Governador Valadares - MG", "+55 33 3271-4400", "florestal@riodoce.com.br"},
                {"55.666.777/0001-55", "Editora e Gráfica Alvorada S.A.", "SIA Trecho 3, Lote 620, Zona Industrial, Brasília - DF", "+55 61 3344-9800", "grafica@alvorada.com.br"},
                {"66.777.888/0001-44", "Frigorífico Boi Gordo do Pantanal Ltda", "Av. Duque de Caxias, 3100, Amambaí, Campo Grande - MS", "+55 67 3384-2200", "qualidade@boigordopantanal.com.br"},
                {"77.888.999/0001-33", "Malharia e Tinturaria Blumenauense S.A.", "Rua Itajaí, 1400, Vorstadt, Blumenau - SC", "+55 47 3326-8800", "textil@blumenauense.ind.br"},
                {"88.999.000/0001-22", "Engenharia de Dutos e Soldas Manaus Ltda", "Distrito Industrial I, Manaus - AM", "+55 92 3615-7700", "dutos@engmanaus.com.br"},
                {"99.000.111/0001-11", "Centro Comercial Plaza Niterói S.A.", "Rua XV de Novembro, 8, Centro, Niterói - RJ", "+55 21 2620-3000", "facilities@plazaniteroi.com.br"},
                {"10.111.222/0001-00", "Companhia Brasileira de Bebidas e Sucos", "Rodovia Dom Pedro I, Km 75, Atibaia - SP", "+55 11 4414-6000", "seguranca@bebidasbrasil.ind.br"},
                {"21.222.333/0001-01", "Terminais de Carga e Armazenagem Recife S.A.", "Av. Engenheiro Abdias de Carvalho, 1500, Prado, Recife - PE", "+55 81 3445-9000", "operacional@recifecargas.com.br"},
                {"32.333.444/0001-02", "Siderúrgica Tubarão do Sul Ltda", "Av. Getúlio Vargas, 2000, Aterrado, Volta Redonda - RJ", "+55 24 3348-1000", "seguranca.trabalho@siderurgicasul.com.br"},
                {"43.444.555/0001-03", "Universidade e Centro de Pesquisa Integrada", "Av. Washington Luís, 5000, Santo Amaro, São Paulo - SP", "+55 11 5543-2000", "infraestrutura@universidadeintegrada.edu.br"},
                {"54.555.666/0001-04", "Shopping Mall Morumbi Sul Empreendimentos", "Av. Giovanni Gronchi, 5819, Vila Andrade, São Paulo - SP", "+55 11 3747-7000", "bombeiros.civis@morumbisul.com.br"},
                {"65.666.777/0001-05", "Refinaria e Distribuidora PetroVale Ltda", "Rodovia BR-101, Km 260, Serra - ES", "+55 27 3328-5000", "brigada@petrovale.com.br"}
        };

        List<Customer> savedCustomers = new ArrayList<>();

        for (String[] data : venefastCustomers) {
            Customer c = new Customer(data[0], data[1], data[2], data[3], data[4]);
            savedCustomers.add(customerRepository.save(c));
        }

        for (String[] data : brazilCustomers) {
            Customer c = new Customer(data[0], data[1], data[2], data[3], data[4]);
            savedCustomers.add(customerRepository.save(c));
        }

        log.info("Inserted 50 customers. Now adding physical customer equipment (active & expired)...");

        // Seed equipment for the first 15 customers to test expired queries & history
        LocalDate today = LocalDate.now();
        String[] types = {"PQS 10 Lbs ABC", "CO2 5 Lbs BC", "Solkaflam 10 Lbs Agente Limpio", "Agua Presurizada 2.5 Gal", "PQS 20 Lbs ABC"};

        for (int i = 0; i < 15; i++) {
            Customer customer = savedCustomers.get(i);
            // Expired equipment
            CustomerEquipment eq1 = new CustomerEquipment(
                    customer,
                    types[i % types.length],
                    today.minusYears(1).minusMonths(2),
                    today.minusMonths(1) // EXPIRED last month
            );
            eq1.setSerialNumber("SN-EXP-" + (1000 + i));
            eq1.setLocation("Pasillo Principal / Recepción");
            eq1.setNotes("Requiere servicio de recarga urgente");
            customerEquipmentRepository.save(eq1);

            // Active equipment
            CustomerEquipment eq2 = new CustomerEquipment(
                    customer,
                    types[(i + 1) % types.length],
                    today.minusMonths(2),
                    today.plusMonths(10) // ACTIVE
            );
            eq2.setSerialNumber("SN-ACT-" + (2000 + i));
            eq2.setLocation("Área de Servidores / Cuarto Eléctrico");
            eq2.setNotes("Equipo en condiciones óptimas");
            customerEquipmentRepository.save(eq2);
        }
    }

    private void seedCatalog() {
        log.info("Seeding 20 assorted catalog items (12 products + 8 services)...");

        List<CatalogItem> items = List.of(
                // 12 Products
                new CatalogItem("EXT-PQS-5", "Extintor Polvo Químico Seco (PQS) 5 Lbs ABC", ItemType.PRODUCT,
                        new BigDecimal("25.00"), "USD", "https://assets.extin.com/catalog/ext-pqs-5.png"),
                new CatalogItem("EXT-PQS-10", "Extintor Polvo Químico Seco (PQS) 10 Lbs ABC", ItemType.PRODUCT,
                        new BigDecimal("38.50"), "USD", "https://assets.extin.com/catalog/ext-pqs-10.png"),
                new CatalogItem("EXT-PQS-20", "Extintor Polvo Químico Seco (PQS) 20 Lbs ABC", ItemType.PRODUCT,
                        new BigDecimal("58.00"), "USD", "https://assets.extin.com/catalog/ext-pqs-20.png"),
                new CatalogItem("EXT-CO2-5", "Extintor Dióxido de Carbono (CO2) 5 Lbs Clase BC", ItemType.PRODUCT,
                        new BigDecimal("65.00"), "USD", "https://assets.extin.com/catalog/ext-co2-5.png"),
                new CatalogItem("EXT-CO2-10", "Extintor Dióxido de Carbono (CO2) 10 Lbs Clase BC", ItemType.PRODUCT,
                        new BigDecimal("95.00"), "USD", "https://assets.extin.com/catalog/ext-co2-10.png"),
                new CatalogItem("EXT-CO2-15", "Extintor Dióxido de Carbono (CO2) 15 Lbs Clase BC", ItemType.PRODUCT,
                        new BigDecimal("135.00"), "USD", "https://assets.extin.com/catalog/ext-co2-15.png"),
                new CatalogItem("EXT-H2O-25", "Extintor de Agua Presurizada 2.5 Galones Clase A", ItemType.PRODUCT,
                        new BigDecimal("48.00"), "USD", "https://assets.extin.com/catalog/ext-h2o-25.png"),
                new CatalogItem("EXT-SOLK-10", "Extintor Agente Limpio Solkaflam 10 Lbs Clase ABC", ItemType.PRODUCT,
                        new BigDecimal("145.00"), "USD", "https://assets.extin.com/catalog/ext-solk-10.png"),
                new CatalogItem("GAB-MET-10", "Gabinete Metálico Rojo de Pared para Extintor 10-20 Lbs", ItemType.PRODUCT,
                        new BigDecimal("32.00"), "USD", "https://assets.extin.com/catalog/gab-met-10.png"),
                new CatalogItem("MANG-INC-30", "Manguera Contra Incendio 1.5 Pulgadas x 30 Metros", ItemType.PRODUCT,
                        new BigDecimal("85.00"), "USD", "https://assets.extin.com/catalog/mang-inc-30.png"),
                new CatalogItem("VALV-ANG-15", "Válvula Angular de Bronce 1.5 Pulgadas 300 PSI", ItemType.PRODUCT,
                        new BigDecimal("42.00"), "USD", "https://assets.extin.com/catalog/valv-ang-15.png"),
                new CatalogItem("DET-HUM-OPT", "Detector Fotoeléctrico Óptico de Humo 9V/12V", ItemType.PRODUCT,
                        new BigDecimal("18.50"), "USD", "https://assets.extin.com/catalog/det-hum-opt.png"),

                // 8 Services
                new CatalogItem("REC-PQS-5", "Servicio de Recarga y Mantenimiento PQS 5 Lbs", ItemType.SERVICE,
                        new BigDecimal("8.50"), "USD", "https://assets.extin.com/catalog/rec-pqs-5.png"),
                new CatalogItem("REC-PQS-10", "Servicio de Recarga y Mantenimiento PQS 10 Lbs", ItemType.SERVICE,
                        new BigDecimal("12.00"), "USD", "https://assets.extin.com/catalog/rec-pqs-10.png"),
                new CatalogItem("REC-PQS-20", "Servicio de Recarga y Mantenimiento PQS 20 Lbs", ItemType.SERVICE,
                        new BigDecimal("18.00"), "USD", "https://assets.extin.com/catalog/rec-pqs-20.png"),
                new CatalogItem("REC-CO2-5", "Servicio de Recarga y Presurización CO2 5 Lbs", ItemType.SERVICE,
                        new BigDecimal("15.00"), "USD", "https://assets.extin.com/catalog/rec-co2-5.png"),
                new CatalogItem("REC-CO2-10", "Servicio de Recarga y Presurización CO2 10 Lbs", ItemType.SERVICE,
                        new BigDecimal("24.00"), "USD", "https://assets.extin.com/catalog/rec-co2-10.png"),
                new CatalogItem("PRU-HID-CIL", "Prueba Hidrostática y Certificación de Cilindro", ItemType.SERVICE,
                        new BigDecimal("20.00"), "USD", "https://assets.extin.com/catalog/pru-hid-cil.png"),
                new CatalogItem("MANT-GAB-MANG", "Mantenimiento Preventivo y Prueba a Gabinete y Manguera", ItemType.SERVICE,
                        new BigDecimal("25.00"), "USD", "https://assets.extin.com/catalog/mant-gab-mang.png"),
                new CatalogItem("CERT-BOMB-ANL", "Inspección Técnica de Bomberos y Certificado de Operatividad", ItemType.SERVICE,
                        new BigDecimal("60.00"), "USD", "https://assets.extin.com/catalog/cert-bomb-anl.png")
        );

        catalogItemRepository.saveAll(items);
        log.info("Inserted 20 catalog items (12 products and 8 services).");
    }

    private void seedQuotesAndInvoices() {
        log.info("Seeding realistic Quotes and Paid Invoices for Postman and API validation...");

        // Fetch sample customers
        Customer elSol = customerRepository.findByTaxId("J-12345678-9").orElse(null);
        Customer paulistana = customerRepository.findByTaxId("12.345.678/0001-90").orElse(null);
        Customer metalCentro = customerRepository.findByTaxId("J-20456789-1").orElse(null);
        Customer losAndes = customerRepository.findByTaxId("J-31567890-2").orElse(null);
        Customer estrelaSul = customerRepository.findByTaxId("34.567.890/0001-12").orElse(null);
        Customer farmacias = customerRepository.findByTaxId("J-50789012-4").orElse(null);

        // Fetch catalog items
        CatalogItem pqs10 = catalogItemRepository.findBySkuCode("EXT-PQS-10").orElse(null);
        CatalogItem pqs5 = catalogItemRepository.findBySkuCode("EXT-PQS-5").orElse(null);
        CatalogItem co25 = catalogItemRepository.findBySkuCode("EXT-CO2-5").orElse(null);
        CatalogItem co210 = catalogItemRepository.findBySkuCode("EXT-CO2-10").orElse(null);
        CatalogItem recPqs10 = catalogItemRepository.findBySkuCode("REC-PQS-10").orElse(null);
        CatalogItem recCo25 = catalogItemRepository.findBySkuCode("REC-CO2-5").orElse(null);
        CatalogItem mangInc30 = catalogItemRepository.findBySkuCode("MANG-INC-30").orElse(null);
        CatalogItem gabMet10 = catalogItemRepository.findBySkuCode("GAB-MET-10").orElse(null);
        CatalogItem detHumOpt = catalogItemRepository.findBySkuCode("DET-HUM-OPT").orElse(null);

        if (elSol == null || pqs10 == null) {
            log.warn("Customers or catalog items not yet available. Skipping quote and invoice seeding.");
            return;
        }

        // =========================================================================
        // 1. SEED QUOTES READY FOR INVOICING (APPROVED & PENDING)
        // =========================================================================

        // Quote 1: APPROVED, ready to be paid & invoiced in VES (Total USD: $104.00)
        Quote quote1 = new Quote();
        quote1.setCustomer(elSol);
        quote1.setStatus(QuoteStatus.APPROVED);
        quote1.setBaseCurrency("USD");
        quote1.setTargetCurrency("VES");
        quote1.setExchangeRate(new BigDecimal("36.5000"));
        quote1.setIssueDate(LocalDateTime.now().minusDays(2));
        quote1.setCustomerCity("Caracas");
        quote1.setCustomerAddress("Av. Francisco de Miranda, Torre Delta, Piso 4, Caracas");
        quote1.setDocumentNumber("003875");
        quote1.setNotes("Cotización aprobada para dotación de extintores en Torre Delta");

        QuoteDetail q1d1 = new QuoteDetail(quote1, pqs10, 2, new BigDecimal("38.50")); // 77.00 USD
        QuoteDetail q1d2 = new QuoteDetail(quote1, recCo25, 1, new BigDecimal("15.00")); // 15.00 USD
        QuoteDetail q1d3 = new QuoteDetail(quote1, recPqs10, 1, new BigDecimal("12.00")); // 12.00 USD
        quote1.addDetail(q1d1);
        quote1.addDetail(q1d2);
        quote1.addDetail(q1d3);
        quote1.recalculateTotals();
        Quote savedQuote1 = quoteRepository.save(quote1);

        // Quote 2: APPROVED, ready to be paid & invoiced in BRL (Total USD: $100.00)
        Quote quote2 = new Quote();
        quote2.setCustomer(paulistana != null ? paulistana : elSol);
        quote2.setStatus(QuoteStatus.APPROVED);
        quote2.setBaseCurrency("USD");
        quote2.setTargetCurrency("BRL");
        quote2.setExchangeRate(new BigDecimal("5.5000"));
        quote2.setIssueDate(LocalDateTime.now().minusDays(1));
        quote2.setCustomerCity("São Paulo");
        quote2.setCustomerAddress("Av. Paulista 1000, Bela Vista, São Paulo");
        quote2.setDocumentNumber("003876");
        quote2.setNotes("Cotação aprovada para compra de extintores PQS 5 Lbs");

        QuoteDetail q2d1 = new QuoteDetail(quote2, pqs5 != null ? pqs5 : pqs10, 4, new BigDecimal("25.00")); // 100.00 USD
        quote2.addDetail(q2d1);
        quote2.recalculateTotals();
        Quote savedQuote2 = quoteRepository.save(quote2);

        // Quote 3: PENDING (Total USD: $275.00)
        Quote quote3 = new Quote();
        quote3.setCustomer(metalCentro != null ? metalCentro : elSol);
        quote3.setStatus(QuoteStatus.PENDING);
        quote3.setBaseCurrency("USD");
        quote3.setTargetCurrency("USD");
        quote3.setExchangeRate(BigDecimal.ONE);
        quote3.setIssueDate(LocalDateTime.now().minusHours(8));
        quote3.setCustomerCity("Maracay");
        quote3.setCustomerAddress("Zona Industrial San Vicente, Maracay, Aragua");
        quote3.setNotes("Cotización pendiente por revisión técnica");

        QuoteDetail q3d1 = new QuoteDetail(quote3, co210 != null ? co210 : pqs10, 2, new BigDecimal("95.00")); // 190.00 USD
        QuoteDetail q3d2 = new QuoteDetail(quote3, mangInc30 != null ? mangInc30 : pqs10, 1, new BigDecimal("85.00")); // 85.00 USD
        quote3.addDetail(q3d1);
        quote3.addDetail(q3d2);
        quote3.recalculateTotals();
        Quote savedQuote3 = quoteRepository.save(quote3);

        log.info(">>> [SEED READY FOR POSTMAN] Quote APPROVED (VES): ID={} | Total USD={}", savedQuote1.getId(), savedQuote1.getTotalAmount());
        log.info(">>> [SEED READY FOR POSTMAN] Quote APPROVED (BRL): ID={} | Total USD={}", savedQuote2.getId(), savedQuote2.getTotalAmount());
        log.info(">>> [SEED READY FOR POSTMAN] Quote PENDING  (USD): ID={} | Total USD={}", savedQuote3.getId(), savedQuote3.getTotalAmount());

        // =========================================================================
        // 2. SEED REALISTIC PAID INVOICES (STATUS: PAID)
        // =========================================================================

        // Invoice 1: Paid in VES via Pago Móvil BDV
        Quote quoteInv1 = new Quote();
        quoteInv1.setCustomer(losAndes != null ? losAndes : elSol);
        quoteInv1.setStatus(QuoteStatus.INVOICED);
        quoteInv1.setBaseCurrency("USD");
        quoteInv1.setTargetCurrency("VES");
        quoteInv1.setExchangeRate(new BigDecimal("45.5000"));
        quoteInv1.setIssueDate(LocalDateTime.now().minusDays(5));
        quoteInv1.setCustomerCity("Mérida");
        quoteInv1.setCustomerAddress("Av. Las Américas, Edif. Los Pinos, Mérida");
        quoteInv1.setDocumentNumber("003877");
        QuoteDetail qi1d1 = new QuoteDetail(quoteInv1, recPqs10 != null ? recPqs10 : pqs10, 3, new BigDecimal("12.00")); // 36.00 USD
        QuoteDetail qi1d2 = new QuoteDetail(quoteInv1, recCo25 != null ? recCo25 : pqs10, 2, new BigDecimal("15.00"));  // 30.00 USD
        quoteInv1.addDetail(qi1d1);
        quoteInv1.addDetail(qi1d2);
        quoteInv1.recalculateTotals(); // Total USD = 66.00
        Quote savedQuoteInv1 = quoteRepository.save(quoteInv1);

        Invoice invoice1 = new Invoice();
        invoice1.setQuote(savedQuoteInv1);
        invoice1.setCustomer(savedQuoteInv1.getCustomer());
        invoice1.setStatus(InvoiceStatus.PAID);
        invoice1.setBaseCurrency("USD");
        invoice1.setTargetCurrency("VES");
        invoice1.setExchangeRate(new BigDecimal("45.5000"));
        BigDecimal totalUsd1 = savedQuoteInv1.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLocal1 = totalUsd1.multiply(new BigDecimal("45.5000")).setScale(2, RoundingMode.HALF_UP); // 66.00 * 45.50 = 3003.00 VES
        invoice1.setSubtotal(savedQuoteInv1.getSubtotal());
        invoice1.setAppliesIva(savedQuoteInv1.getAppliesIva());
        invoice1.setTaxRate(savedQuoteInv1.getTaxRate());
        invoice1.setTaxAmount(savedQuoteInv1.getTaxAmount());
        invoice1.setTotalAmountUsd(totalUsd1);
        invoice1.setTotalAmountLocal(totalLocal1);
        LocalDateTime rateDate1 = LocalDateTime.now().minusDays(3);
        invoice1.setRateDate(rateDate1);
        invoice1.setIssueDate(rateDate1);
        invoice1.setPaymentReference("PAGOMOVIL-BDV-98765432");
        invoice1.setFiscalControlNumber("INV-VES-2026-0001");

        InvoiceDetail id1_1 = new InvoiceDetail(invoice1, recPqs10 != null ? recPqs10 : pqs10, 3, new BigDecimal("12.00"));
        InvoiceDetail id1_2 = new InvoiceDetail(invoice1, recCo25 != null ? recCo25 : pqs10, 2, new BigDecimal("15.00"));
        invoice1.addDetail(id1_1);
        invoice1.addDetail(id1_2);
        Invoice savedInvoice1 = invoiceRepository.save(invoice1);

        // Invoice 2: Paid in BRL via PIX Banco do Brasil
        Quote quoteInv2 = new Quote();
        quoteInv2.setCustomer(estrelaSul != null ? estrelaSul : elSol);
        quoteInv2.setStatus(QuoteStatus.INVOICED);
        quoteInv2.setBaseCurrency("USD");
        quoteInv2.setTargetCurrency("BRL");
        quoteInv2.setExchangeRate(new BigDecimal("5.6000"));
        quoteInv2.setIssueDate(LocalDateTime.now().minusDays(2));
        QuoteDetail qi2d1 = new QuoteDetail(quoteInv2, co25 != null ? co25 : pqs10, 2, new BigDecimal("65.00")); // 130.00 USD
        QuoteDetail qi2d2 = new QuoteDetail(quoteInv2, gabMet10 != null ? gabMet10 : pqs10, 2, new BigDecimal("32.00")); // 64.00 USD
        quoteInv2.addDetail(qi2d1);
        quoteInv2.addDetail(qi2d2);
        quoteInv2.recalculateTotals(); // Total USD = 194.00
        Quote savedQuoteInv2 = quoteRepository.save(quoteInv2);

        Invoice invoice2 = new Invoice();
        invoice2.setQuote(savedQuoteInv2);
        invoice2.setCustomer(savedQuoteInv2.getCustomer());
        invoice2.setStatus(InvoiceStatus.PAID);
        invoice2.setBaseCurrency("USD");
        invoice2.setTargetCurrency("BRL");
        invoice2.setExchangeRate(new BigDecimal("5.6000"));
        BigDecimal totalUsd2 = savedQuoteInv2.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLocal2 = totalUsd2.multiply(new BigDecimal("5.6000")).setScale(2, RoundingMode.HALF_UP); // 194.00 * 5.60 = 1086.40 BRL
        invoice2.setSubtotal(savedQuoteInv2.getSubtotal());
        invoice2.setAppliesIva(savedQuoteInv2.getAppliesIva());
        invoice2.setTaxRate(savedQuoteInv2.getTaxRate());
        invoice2.setTaxAmount(savedQuoteInv2.getTaxAmount());
        invoice2.setTotalAmountUsd(totalUsd2);
        invoice2.setTotalAmountLocal(totalLocal2);
        LocalDateTime rateDate2 = LocalDateTime.now().minusDays(1);
        invoice2.setRateDate(rateDate2);
        invoice2.setIssueDate(rateDate2);
        invoice2.setPaymentReference("PIX-BB-88331122");
        invoice2.setFiscalControlNumber("INV-BRL-2026-0002");

        InvoiceDetail id2_1 = new InvoiceDetail(invoice2, co25 != null ? co25 : pqs10, 2, new BigDecimal("65.00"));
        InvoiceDetail id2_2 = new InvoiceDetail(invoice2, gabMet10 != null ? gabMet10 : pqs10, 2, new BigDecimal("32.00"));
        invoice2.addDetail(id2_1);
        invoice2.addDetail(id2_2);
        Invoice savedInvoice2 = invoiceRepository.save(invoice2);

        // Invoice 3: Paid in USD via Zelle
        Quote quoteInv3 = new Quote();
        quoteInv3.setCustomer(farmacias != null ? farmacias : elSol);
        quoteInv3.setStatus(QuoteStatus.INVOICED);
        quoteInv3.setBaseCurrency("USD");
        quoteInv3.setTargetCurrency("USD");
        quoteInv3.setExchangeRate(BigDecimal.ONE);
        quoteInv3.setIssueDate(LocalDateTime.now().minusHours(12));
        QuoteDetail qi3d1 = new QuoteDetail(quoteInv3, detHumOpt != null ? detHumOpt : pqs10, 10, new BigDecimal("18.50")); // 185.00 USD
        quoteInv3.addDetail(qi3d1);
        quoteInv3.recalculateTotals(); // Total USD = 185.00
        Quote savedQuoteInv3 = quoteRepository.save(quoteInv3);

        Invoice invoice3 = new Invoice();
        invoice3.setQuote(savedQuoteInv3);
        invoice3.setCustomer(savedQuoteInv3.getCustomer());
        invoice3.setStatus(InvoiceStatus.PAID);
        invoice3.setBaseCurrency("USD");
        invoice3.setTargetCurrency("USD");
        invoice3.setExchangeRate(BigDecimal.ONE);
        BigDecimal totalUsd3 = savedQuoteInv3.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        invoice3.setSubtotal(savedQuoteInv3.getSubtotal());
        invoice3.setAppliesIva(savedQuoteInv3.getAppliesIva());
        invoice3.setTaxRate(savedQuoteInv3.getTaxRate());
        invoice3.setTaxAmount(savedQuoteInv3.getTaxAmount());
        invoice3.setTotalAmountUsd(totalUsd3);
        invoice3.setTotalAmountLocal(totalUsd3);
        LocalDateTime rateDate3 = LocalDateTime.now().minusHours(5);
        invoice3.setRateDate(rateDate3);
        invoice3.setIssueDate(rateDate3);
        invoice3.setPaymentReference("ZELLE-REF-55443322");
        invoice3.setFiscalControlNumber("INV-USD-2026-0003");

        InvoiceDetail id3_1 = new InvoiceDetail(invoice3, detHumOpt != null ? detHumOpt : pqs10, 10, new BigDecimal("18.50"));
        invoice3.addDetail(id3_1);
        Invoice savedInvoice3 = invoiceRepository.save(invoice3);

        log.info(">>> [SEED INVOICE PAID] Invoice 1 (VES): ID={} | Total VES={} | Ref={}", savedInvoice1.getId(), savedInvoice1.getTotalAmountLocal(), savedInvoice1.getPaymentReference());
        log.info(">>> [SEED INVOICE PAID] Invoice 2 (BRL): ID={} | Total BRL={} | Ref={}", savedInvoice2.getId(), savedInvoice2.getTotalAmountLocal(), savedInvoice2.getPaymentReference());
        log.info(">>> [SEED INVOICE PAID] Invoice 3 (USD): ID={} | Total USD={} | Ref={}", savedInvoice3.getId(), savedInvoice3.getTotalAmountLocal(), savedInvoice3.getPaymentReference());
    }
}
