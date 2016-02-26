package logica;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Years;

public class Inscripcion {
	static Connection con; //Creo la conexión
	
	public static void main(String[] args) throws IOException, ParseException {
		try {
			con = DriverManager.getConnection("jdbc:ucanaccess://src/SINFDB.accdb");
			BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			/* PROBANDO 
			 * GIT 
			 */
			System.out.println("----_____----- ¡¡¡ CARRERAS POPULARES Y DE TRAIL !!! -----_____----");
			System.out.println("\t");
			System.out.println("Inserte su DNI: ");
		   	String dni = bufferRead.readLine();
		   	
		   	if(comprobarsiExiste(dni)==true){
				System.out.println("Ya se encuentra registrado");
				System.exit(0);
			}
			else{
				System.out.println("No se encuentra registrado, continue introduciendo el resto de sus datos personales");
			}
			System.out.println("Inserte su nombre: ");
		   	String nombre = bufferRead.readLine();
		   	
		   	System.out.println("Inserte sus apellidos: ");
		   	String apellidos = bufferRead.readLine();
		  	
			System.out.println("Inserte su sexo: ");
		   	String sexo = bufferRead.readLine();
			
			DateTime fechaAux;
			LocalDate fecLD;
			while (true) {
				System.out.println("Introduzca su fecha de nacimiento: [DD/MM/AAAA]");
				String f = bufferRead.readLine(); // POR TECLADO
				// String f = "21/05/1993";
				if (f.split("/").length != 3)
					continue;
				int day = Integer.parseInt(f.split("/")[0]);
				if (day <= 0 || day > 31)
					continue;
				int month = Integer.parseInt(f.split("/")[1]);
				if (month <= 0 || month > 12)
					continue;
				int year = Integer.parseInt(f.split("/")[2]);
				if ((year <= LocalDate.now().getYear() - 100) || (year > LocalDate.now().getYear()))
					continue;
				fechaAux = new DateTime(year, month, day, 0, 0);
				fecLD = new LocalDate(year,month,day);
				break;
			}
			java.sql.Date fechaNaci = new java.sql.Date(fechaAux.getMillis());
			
			//CALCULA LA FECHA ACTUAL
			DateTime factual;
			int año= LocalDate.now().getYear();
			int mes= LocalDate.now().getMonthOfYear();
			int dia= LocalDate.now().getDayOfMonth();
			factual = new DateTime(año, mes, dia, 0, 0);
			java.sql.Date factual1 = new java.sql.Date(factual.getMillis());
			
			System.out.println("Inserte la forma de pago(SOLO EL NÚMERO):"
					+ " 1-Transferencia Bancaria 2-Tarjeta de Credito\n");
			Scanner teclado = new Scanner(System.in);
			int caso = teclado.nextInt();
			String estado = "";
			String formaPago = "";
			int cuota = 0;
			switch(caso){
			case 1:
				estado="preinscrito";
				formaPago="Transferencia Bancaria";
				cuota=calcularCuota(factual1);
				System.out.printf("Debe abonar:%d€ (Tiene dos días para pagar!!)",cuota);
				break;
			case 2:
				estado="inscrito";
				formaPago="Tarjeta Credito";
				cuota=calcularCuota(factual1);
				System.out.printf("Debe abonar:%d€",cuota);
				break;
			}
		   	LocalDate fComp = new LocalDate(2016, 9, 15);
		   	String nombreCompe= "Trail Blanco Valgrande-Pajares";
			String DNIatleta = dni;
			String categoria = calcularCategoria(fecLD, sexo, nombreCompe, fComp);
			
			
			//PARAMETROS PARA AÑADIR A LA TABLA COMPETICION
			int numPlazas = 10;
			
			//INSERTAR LOS DATOS ANTERIORES EN LA TABLA ATLETA
			añadirRegistro(nombre, apellidos,  dni, fechaNaci,sexo);
			
			//TEMA PLAZOS
			//comprueba si en esa competicion (nombreCompe) y con la fecha actual(factual1):
			//SE PUEDE O NO INSCRIBIR, teniendo en cuenta los plazos mirados dentro del metodo establecerPlazos
			
			System.out.println("ESTABLECER PLAZOS");
			
			if (establecerPlazos(nombreCompe,factual1)==true){
				añadirRegistroInscripcion(factual1,estado, cuota, formaPago,DNIatleta,categoria,nombreCompe);
				System.out.println("Se encuentra entre los plazos establecidos, ha sido inscrito en la carrera: " + nombreCompe);
				System.out.println("El estado de su inscripcion es de: " + estado);
								System.exit(0);
		}
			else{
				System.out.println("Se encuentra fuera de plazo, NO puede inscribirse");
			}			
		} catch (SQLException e) {
			System.out.println("ERROR EN USERMANAGER");
			e.printStackTrace();
		}	
	}
	
	/**
	 * @author beatriz
	 * Metodo que comprueba si el dni introducido por el atleta ya existe en la base de datos. 
	 * @param dni
	 * @return
	 * @throws IOException
	 */
	private static boolean comprobarsiExiste(String dni) throws IOException{
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		Statement s;
		ResultSet rs;
	
		try {
			//CONSULTAR en la base de datos que el nombre que me dan esta registrado en el atleta.
			String consulta = "SELECT Atleta.DNI FROM Atleta INNER JOIN Inscripcion ON Atleta.DNI = Inscripcion.DNIAtleta where Atleta.DNI='"+dni+"';"; 

			s = con.createStatement();
			rs = s.executeQuery(consulta);
		
			if(rs.next()){
				return true;
			}
			return false;

			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
		
	}

	/**
	 * @author beatriz
	 * Metodo que inserta en la tabla Atleta los datos personales introducidos por el usuario. 
	 * @param Nombre
	 * @param Apellidos
	 * @param DNI
	 * @param fecha
	 * @param Sexo
	 */

	private static void añadirRegistro(String Nombre, String Apellidos, String DNI, java.sql.Date fecha, String Sexo){
		try {
			String consulta = "insert into Atleta (Nombre,Apellidos,DNI,FechaNacimiento,Sexo) values(?,?,?,?,?)";
			PreparedStatement ps = con.prepareStatement(consulta);
			ps.setString(1, Nombre);
			ps.setString(2, Apellidos );
			ps.setString(3, DNI);
			ps.setDate(4, fecha);
			ps.setString(5, Sexo);
			ps.executeUpdate();
			
		} catch (SQLException e) {
			System.out.println("ERROR EN añadirRegistro");
			e.printStackTrace();
		}
	}
	
	/**
	 * @author beatriz
	 * Metodo que inserta en la tabla Inscripcion todos sus campos. 
	 * @param IdInscrip
	 * @param fechaIns
	 * @param estado
	 * @param cuota
	 * @param formaPago
	 * @param DNIatleta
	 * @param categoria
	 * @param nombreCompe
	 */
	private static void añadirRegistroInscripcion(java.sql.Date fechaIns, String estado, int cuota, String formaPago, String DNIatleta, String categoria, String nombreCompe){
		try {
		String consulta = "insert into Inscripcion (FechaInscripcion,Estado,CuotaAbonada,FormaPago,DNIAtleta,CategoriaInscripcion,NombreCompeticion) values(?,?,?,?,?,?,?)";
		PreparedStatement ps = con.prepareStatement(consulta);
		ps.setDate(1, fechaIns);
		ps.setString(2,estado);
		ps.setInt(3, cuota);
		ps.setString(4,formaPago);
		ps.setString(5, DNIatleta);
		ps.setString(6, categoria);
		ps.setString(7, nombreCompe);
		ps.executeUpdate();
		} catch (SQLException e) {
			System.out.println("ERROR EN añadirRegistroInscripcion");
			e.printStackTrace();
		}
	}
	
	/**
	 * @author beatriz
	 * Metodo que comprueba que el atleta esta dentro de los plazos establecidos
	 * en la competicion para poder inscribirse.
	 * @param nombreCompeticion 
	 * @param fecha
	 * @return
	 */
	private static boolean establecerPlazos(String nombreCompeticion, java.sql.Date fecha){	
		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
		try {
		String consulta = "SELECT Plazo.IdPlazo from Plazo where Plazo.NombreCompeticion= ? AND  FechaInicio <= ? and FechaFin > ?";
		PreparedStatement ps = con.prepareStatement(consulta);
		ps.setString(1, nombreCompeticion);
		ps.setDate(2, fecha);
		ps.setDate(3, fecha);
		ResultSet rs = ps.executeQuery();
		
		if(rs.next()){
			return true;
		}
		return false;
	} catch (SQLException e) {
		e.printStackTrace();
	}
	return false;
	
	}
	
	
	/**
	 * @author mbayon
	 * @param fNaci Fecha de nacimiento del atleta
	 * @param sexo Sexo del Atleta
	 * @param nComp Nombre de la competición
	 * @param fComp Fecha de inicio de la competición
	 * @return Categoría a la que pertenece el Atleta o null si no la encuentra
	 */
	public static String calcularCategoria(LocalDate fNaci, String sexo, String nComp, LocalDate fComp) {
		//COMPROBAR ERRORES EN LOS PARÁMETROS?
		try {
			// Calcula la edad del Atleta respecto a la fecha de la competicion
			int age = Years.yearsBetween(fNaci, fComp).getYears();
			if (age <= 0 || age > 200)
				return null;
			// Consulto la categoria correspondiente a la edad, el sexo y la
			// competicion
			String cons = "SELECT Categoria.NombreCategoria FROM Categoria INNER JOIN ((Competicion INNER JOIN Inscripcion ON "
					+ "Competicion.NombreCompeticion = Inscripcion.NombreCompeticion) INNER JOIN CompeticionCategoria ON "
					+ "Competicion.NombreCompeticion = CompeticionCategoria.NombreCompeticion) ON Categoria.NombreCategoria = "
					+ "CompeticionCategoria.NombreCategoria WHERE Categoria.AñoInicio < ? AND Categoria.AñoFin > ? "
					+ "AND Categoria.Sexo = ? AND CompeticionCategoria.NombreCompeticion = ?";
			PreparedStatement ps = con.prepareStatement(cons);
			ps.setInt(1, age);
			ps.setInt(2, age);
			ps.setString(3, sexo);
			ps.setString(4, nComp);
			// Retorno el nombre de la categoria o null si no la encuentro
			ResultSet rs = ps.executeQuery();
			String cat = "";
			while (rs.next()) {
				cat = rs.getString(1);
			}
			if (cat == "")
				return null;
			return cat;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * @author mbayon
	 * @param nomComp Nombre de la competición
	 * @return ResultSet con todos los inscritos en la competición
	 */
	public static ResultSet consultarInscritos(String nomComp){
		try {
			String cons = "SELECT Inscripcion.IdInscripcion, Atleta.DNI, Atleta.Apellidos, Atleta.Nombre, "
					+ "Inscripcion.CategoriaInscripcion, Inscripcion.FechaInscripcion, Inscripcion.CuotaAbonada, "
					+ "Inscripcion.Estado FROM Atleta INNER JOIN Inscripcion ON Atleta.DNI = Inscripcion.DNIAtleta "
					+ "WHERE Inscripcion.NombreCompeticion = ?";
			PreparedStatement ps = con.prepareStatement(cons);
			ps.setString(1, nomComp);
			ResultSet rs = ps.executeQuery();

			return rs;
			
			// MANEJO DEL RESULTSET POR PARTE DE LA INTERFAZ
//			Vector<String> columnNames = new Vector<String>();
//	        columnNames.add("IdInscripcion");	columnNames.add("DNI");
//	        columnNames.add("Apellidos");		columnNames.add("Nombre");
//	        columnNames.add("Categoria");	columnNames.add("FechaInscripcion");
//	        columnNames.add("CuotaAbonada");		columnNames.add("EstadoInscripción");
//	        
//			Vector<Vector<String>> tab = new Vector<Vector<String>>();
//			while (rs.next()) {
//				Vector<String> vstr = new Vector<String>();
//                vstr.add(rs.getString("Inscripcion.IdInscripcion"));
//                vstr.add(rs.getString("Atleta.DNI"));
//                vstr.add(rs.getString("Atleta.Apellidos"));
//                vstr.add(rs.getString("Atleta.Nombre"));
//                vstr.add(rs.getString("Inscripcion.CategoriaInscripcion"));
//                vstr.add(rs.getString("Inscripcion.FechaInscripcion"));
//                vstr.add(rs.getString("Inscripcion.CuotaAbonada"));
//                vstr.add(rs.getString("Inscripcion.Estado"));
//                vstr.add("\n\n\n\n\n\n\n");
//
//                tab.add(vstr);
//			}
//			DefaultTableModel model = new DefaultTableModel(tab, columnNames);
//			return model;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}
	/**
	 * @author pablo y ruben
	 * @param DNI DNI del atleta 
	 * @return Cuota que debe pagar el atleta 
	 */
	public static int calcularCuota(java.sql.Date fecha_ins) {
		int cuota = 0;
		try {
			/*String consulta,fecha_ins = "";
			Statement s;
			ResultSet rs;

			//Consultamos la fecha de inscri pcion del atleta con el dni que nos pasan

			consulta = "SELECT Inscripcion.FechaInscripcion " 
					+ "FROM Atleta INNER JOIN Inscripcion ON '"+dni+"' = Inscripcion.DNIAtleta";
			s = con.createStatement();
			rs = s.executeQuery(consulta);

			while (rs.next()) {
				fecha_ins = rs.getString(1);
			}
			System.out.printf("Fecha Inscripcion: %s\n",fecha_ins);
			if (fecha_ins == ""){
				System.out.printf("La fecha de inscripción para el usuario con DNI: %s no existe\n",dni);
				return 0;
			}*/

			
			/////////////////FALTA EN EL WHERE CON EL NOMBRE DE LA COMPETICION!!!!!!!
			
			String consulta;
			// Consulto la cuota respecto a la fecha de inscripcion
			//la fecha hay que pasarla con comillas simples
			consulta = "SELECT Plazo.Cuota "
					+ "FROM (Competicion INNER JOIN Inscripcion ON Competicion.NombreCompeticion = Inscripcion.NombreCompeticion) "
					+ "INNER JOIN Plazo ON Competicion.NombreCompeticion = Plazo.NombreCompeticion "
					+ "WHERE ? >= Plazo.FechaInicio "
					+ "AND ? < Plazo.FechaFin";

			PreparedStatement ps = con.prepareStatement(consulta);
			ps.setDate(1, fecha_ins);
			ps.setDate(2, fecha_ins);

			// Devolvemos la cuota
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				cuota = Integer.parseInt(rs.getString(1));
			}
			//Si le pasamos una fecha de inscripcion no válida que cuota devuelve??
			if(cuota == 0){
				System.out.print("No esta dentro del plazo \n");
				return 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return cuota;
	}
	
//	/**
//	 * @author Rubén Luque
//	 * @param dni
//	 * @return 
//	 */
//	public static int pagoTransferencia(String dni) {
//		/**
//		 * 1-) Marcamos al atleta como pre-inscrito
//		 * 2-) Reservamos plaza hasta dos días despues de Finscripcion
//		 * 3-) Calculamos su cuota
//		 * **/
//			try
//			{
//			
//			 // create our java preparedstatement using a sql update query
//				PreparedStatement ps = con.prepareStatement(
//						"UPDATE Inscripcion SET Estado = 'Pre-inscrito', FormaPago = 'Transferencia' WHERE DNIAtleta = "+"'"+dni+"'");
//
//				// call executeUpdate to execute our sql update statement
//				ps.executeUpdate();
//				int cuota = calcularCuota(dni);
//				System.out.printf("El atleta con DNI: %s ha seleccionado forma de pago por transferencia \n"
//						+ "Debes pagar: %s euros\n"
//						+ "¡Recuerda abonarlo antes de 2 días desde hoy o su reserva será cancelada!\n",dni,cuota);
//				/**
//				//pagamos
//				ps =  con.prepareStatement(
//						"UPDATE Inscripcion SET Reservado ='false',Estado='Inscrito',CuotaAbonada="+"'"+cuota+"'"+" WHERE DNIAtleta = "+"'"+dni+"'");
//				*/ps.close();
//				
//			}
//			catch (SQLException se)
//			{
//				se.printStackTrace();
//			}
//			return 0;
//	}
}
