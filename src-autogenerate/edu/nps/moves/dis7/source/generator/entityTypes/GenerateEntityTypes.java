/**
 * Copyright (c) 2008-2021, MOVES Institute, Naval Postgraduate School (NPS). All rights reserved.
 * This work is provided under a BSD open-source license, see project license.html and license.txt
 */
package edu.nps.moves.dis7.source.generator.entityTypes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenerateEntityTypes.java created on Jul 22, 2019
 * MOVES Institute, Naval Postgraduate School (NPS), Monterey California USA https://www.nps.edu
 *
 * @author Don McGregor, Mike Bailey and Don Brutzman
 * @version $Id$
 */
public class GenerateEntityTypes
{
    // set defaults to allow direct run
    private        File   outputDirectory;
    private static String outputDirectoryPath = "src-generated/java/edu/nps/moves/dis7/entityTypes"; // default
    private static String         packageName =                    "edu.nps.moves.dis7.entityTypes"; // default
    private static String            language = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_LANGUAGE;
    private static String         sisoXmlFile = edu.nps.moves.dis7.source.generator.GenerateOpenDis7JavaPackages.DEFAULT_SISO_XML_FILE;
    private static String       sisoSpecificationTitleDate = "";

  private BufferedWriter uid2ClassWriter = null;
    
  String entityCommonTemplate;
  String uidFactoryTemplate;

  class DataPkt
  {
    String pkg;
    File directory;
    StringBuilder sb;
    
    private String fullName;
    private String countryNm;
    private String entKindNm;
    private String domainName;
    private String domainValue;
    private String clsNm;
    private String countryNamePretty;
    //private String domainPrettyName;
    private String entKindNmDescription;
    private String entityUid;
  }
    
    private String        packageInfoPath;
    private File          packageInfoFile;
    private StringBuilder packageInfoBuilder;

  /** Constructor for GenerateEntityTypes
     * @param xmlFile sisoXmlFile
     * @param outputDir outputDirectoryPath
     * @param packageName key to package name for entity types */
  public GenerateEntityTypes(String xmlFile, String outputDir, String packageName)
  {
        if (!xmlFile.isEmpty())
             sisoXmlFile = xmlFile;
        if (!outputDir.isEmpty())
            outputDirectoryPath = outputDir;
        if (!packageName.isEmpty())
           GenerateEntityTypes.packageName = packageName;
        System.out.println (GenerateEntityTypes.class.getName());
        System.out.println ("              xmlFile=" + sisoXmlFile);
        System.out.println ("          packageName=" + GenerateEntityTypes.packageName);
        System.out.println ("  outputDirectoryPath=" + outputDirectoryPath);
        
        outputDirectory  = new File(outputDirectoryPath);
        outputDirectory.mkdirs();
//      FileUtils.cleanDirectory(outputDirectory); // do NOT clean directory, results can co-exist with other classes
        System.out.println ("actual directory path=" + outputDirectory.getAbsolutePath());
        
        packageInfoPath = outputDirectoryPath + "/" + "package-info.java";
        packageInfoFile = new File(packageInfoPath);
        
        FileWriter packageInfoFileWriter;
        try {
            packageInfoFile.createNewFile();
            packageInfoFileWriter = new FileWriter(packageInfoFile, StandardCharsets.UTF_8);
            packageInfoBuilder = new StringBuilder();
            packageInfoBuilder.append("/**\n");
            packageInfoBuilder.append(" * Infrastructure classes for ").append(sisoSpecificationTitleDate).append(" enumerations supporting <a href=\"https://github.com/open-dis/open-dis7-java\" target=\"open-dis7-java\">open-dis7-java</a> library.\n");
            packageInfoBuilder.append("\n");
            packageInfoBuilder.append(" * <p> Online: NPS <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500\" target=\"MV3500\">MV3500 Networked Simulation course</a> \n");
            packageInfoBuilder.append(" * links to <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/specifications/README.md\" target=\"README.MV3500\">IEEE and SISO specification references</a> of interest. </p>\n");
            packageInfoBuilder.append(" * <ul> <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=46172\" target=\"SISO-REF-010\" >SISO-REF-010-2020 Reference for Enumerations for Simulation Interoperability</a> </li> \n");
            packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=47284\" target=\"SISO-REF-10.1\">SISO-REF-10.1-2019 Reference for Enumerations for Simulation, Operations Manual</a></li> </ul>\n");
            packageInfoBuilder.append("\n");
            packageInfoBuilder.append(" * @see java.lang.Package\n");
            packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful</a>\n");
            packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java</a>\n");
            packageInfoBuilder.append(" */\n");
            packageInfoBuilder.append("\n");
            packageInfoBuilder.append("package edu.nps.moves.dis7.entities;\n");

            packageInfoFileWriter.write(packageInfoBuilder.toString());
            packageInfoFileWriter.flush();
            packageInfoFileWriter.close();
            System.out.println("Created " + packageInfoPath);
        }
        catch (IOException ex) {
            System.out.flush(); // avoid intermingled output
            System.err.println (ex.getMessage()
               + packageInfoFile.getAbsolutePath()
            );
            ex.printStackTrace(System.err);
        }
  }

  Method methodPlatformDomainFromInt;
  Method methodPlatformDomainName;
  Method methodPlatformDomainDescription;
  
  Method countryFromInt;
  Method countryName;
  Method countryDescription;
  
  Method kindFromInt;
  Method kindName;
  Method kindDescription;

  Method munitionDomainFromInt;
  Method munitionDomainName;
  Method munitionDomainDescription;
  
  Method supplyDomainFromInt;
  Method supplyDomainName;
  Method supplyDomainDescription;

  // Don't put imports in code for this, needs to have enumerations built first; do it this way
  // Update, this might now be unnecessary with the re-structuring of the projects
  private void buildKindDomainCountryInstances()
  {
    Method[] ma;
    try {
      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.PlatformDomain");
      methodPlatformDomainFromInt     = ma[FORVALUE];
      methodPlatformDomainName        = ma[NAME];
      methodPlatformDomainDescription = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.Country");
      countryFromInt     = ma[FORVALUE];
      countryName        = ma[NAME];
      countryDescription = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.EntityKind");
      kindFromInt     = ma[FORVALUE];
      kindName        = ma[NAME];
      kindDescription = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.MunitionDomain");
      munitionDomainFromInt     = ma[FORVALUE];
      munitionDomainName        = ma[NAME];
      munitionDomainDescription = ma[DESCRIPTION];

      ma = getEnumMethods("edu.nps.moves.dis7.enumerations.SupplyDomain");
      supplyDomainFromInt     = ma[FORVALUE];
      supplyDomainName        = ma[NAME];
      supplyDomainDescription = ma[DESCRIPTION];
    }
    catch (Exception ex) {
      throw new RuntimeException(ex.getClass().getName() + ": " + ex.getLocalizedMessage());
    }
  }
  private static final int FORVALUE = 0;
  private static final int NAME = 1;
  private static final int DESCRIPTION = 2;

  private Method[] getEnumMethods(String className) throws Exception
  {
    Method[] ma = new Method[3];

    Class<?> cls = Class.forName(className);
    ma[FORVALUE] = cls.getDeclaredMethod("getEnumForValue", int.class);
    ma[NAME] = cls.getMethod("name", (Class[]) null);
    ma[DESCRIPTION] = cls.getDeclaredMethod("getDescription", (Class[]) null);

    return ma;
  }

  String getDescription(Method enumGetter, Method descriptionGetter, int i) throws Exception
  {
      String result = "";
      if ((enumGetter == null) || (descriptionGetter == null))
          return result;
      try 
      {
        Object enumObj = getEnum(enumGetter, i);
        result = (String) descriptionGetter.invoke(enumObj, (Object[]) null);
      }
      catch (Exception e)
      {
          System.err.println (this.getClass().getName() + ".getDescription() exception:" + e);
      }
      return result;
  }

  String getName(Method enumGetter, Method nameGetter, int i) throws Exception
  {
      if (enumGetter == null)
          return ""; // TODO fix
    Object enumObj = getEnum(enumGetter, i);
    return (String) nameGetter.invoke(enumObj, (Object[]) null);
  }

  Object getEnum(Method enumGetter, int i) throws Exception
  {
    if (enumGetter == null)
    {
        System.err.println ("NPE: getEnum (enumGetter == null, i=" + i + ")");
        return null;
    }
    return enumGetter.invoke(null, i);
  }

  private void run() throws SAXException, IOException, ParserConfigurationException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);

    loadTemplates();
    buildKindDomainCountryInstances(); // TODO questionable invocation, now working...
    
    System.out.println("Generating entities:");
    MyHandler handler = new MyHandler();
    factory.newSAXParser().parse(new File(sisoXmlFile), handler);
    
    if(uid2ClassWriter != null) 
    {
       uid2ClassWriter.flush();
       uid2ClassWriter.close();
    }
    saveUidFactory();
    System.out.println (GenerateEntityTypes.class.getName() + " complete."); // TODO  + handler.enums.size() + " enums created.");
  }

  private void loadTemplates()
  {
    try {
      entityCommonTemplate = loadOneTemplate("entitytypecommon.txt");
      uidFactoryTemplate   = loadOneTemplate("uidfactory.txt");
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String loadOneTemplate(String s) throws Exception
  {
    return new String(Files.readAllBytes(Paths.get(getClass().getResource(s).toURI())), StandardCharsets.UTF_8.name());
  }
  
  private void saveUidFactory()
  {
    saveFile(outputDirectory, "EntityTypeFactory.java",uidFactoryTemplate);
  }
  
  class DescriptionElem
  {
    String description         = new String();
    String pkgFromDescription  = new String();
    String enumFromDescription = new String();
    ArrayList<DescriptionElem> children = new ArrayList<>();
    String value               = new String();
    String uid                 = new String();
  }

  class EntityElem
  {
    String kind    = new String();
    String domain  = new String();
    String country = new String();
    String uid     = new String();
    List<DescriptionElem> categories = new ArrayList<>();
  }

  class CategoryElem extends DescriptionElem
  {
    EntityElem parent;
  }

  class SubCategoryElem extends DescriptionElem
  {
    CategoryElem parent;
  }

  class SpecificElem extends DescriptionElem
  {
    SubCategoryElem parent;
  }

  class ExtraElem extends DescriptionElem
  {
    SpecificElem parent;
  }

  /** XML handler for recursively reading information and autogenerating code, namely an
     * inner class that handles the SAX parsing of the XML file. This is relatively simple, if
     * a little verbose. Basically we just create the appropriate objects as we come across the
     * XML elements in the file.
     */
  public class MyHandler extends DefaultHandler
  {
    ArrayList<EntityElem> entities = new ArrayList<>();
    EntityElem            currentEntity;
    CategoryElem          currentCategory;
    SubCategoryElem       currentSubCategory;
    SpecificElem          currentSpecific;
    ExtraElem             currentExtra;
    boolean               inCot  = false;   // we don't want categories, subcategories, etc. from this group
    boolean               inCetUid30 = false;
    int                   filesWrittenCount = 0;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
      if (inCot)   // don't want anything in this group
        return;

      if (attributes.getValue("deprecated") != null)
        return;
      
      if(qName.equalsIgnoreCase("revision")) {
        if(sisoSpecificationTitleDate.isEmpty()) // only want the first/latest
           sisoSpecificationTitleDate = legalJavaDoc(attributes.getValue("title") + " (" + attributes.getValue("date") + ")");
        return;
      }
      if (qName.equalsIgnoreCase("cet")) {
        if (attributes.getValue("uid").equalsIgnoreCase("30"))
          inCetUid30 = true;
        return;
      }
      // must follow setting of flag above
      if (!inCetUid30) // only want entities in this group, uid 30
        return;
      
      switch (qName) 
      {
        case "cot":
          inCot = true; // cot element, object types
          break;

        case "entity":
          currentEntity         = new EntityElem();
          currentEntity.kind    = attributes.getValue("kind");
          currentEntity.domain  = attributes.getValue("domain");
          currentEntity.country = attributes.getValue("country");
          currentEntity.uid     = attributes.getValue("uid");
          
          if (currentCategory != null) // attribute order independent
          {
                currentCategory.parent = currentEntity;
                setUniquePackageAndEmail(currentCategory, currentEntity.categories);
                currentEntity.categories.add(currentCategory);
          }
          entities.add(currentEntity);
          break;

        case "category":
          currentCategory                  = new CategoryElem();
          currentCategory.value            = attributes.getValue("value");
          currentCategory.description      = attributes.getValue("description");
          if (currentCategory.description != null)
              currentCategory.description  = currentCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentCategory.uid              = attributes.getValue("uid");
          
          if (currentSubCategory != null) // attribute order independent
          {
                currentSubCategory.parent  = currentCategory;
                setUniquePackageAndEmail(currentSubCategory, currentCategory.children);
                currentCategory.children.add(currentSubCategory);
          }
          if (currentEntity != null) // attribute order independent
          {
                currentCategory.parent = currentEntity;
                setUniquePackageAndEmail(currentCategory, currentEntity.categories);
                currentEntity.categories.add(currentCategory);
          }
          break;

        case "subcategory_xref":
          printUnsupportedContainedELementMessage("subcategory_xref", attributes.getValue("description"), currentCategory);
          break;

        case "subcategory_range":
          printUnsupportedContainedELementMessage("subcategory_range", attributes.getValue("description"), currentCategory);
          break;
          
        case "subcategory":
          currentSubCategory                  = new SubCategoryElem();
          currentSubCategory.value            = attributes.getValue("value");
          currentSubCategory.description      = attributes.getValue("description");
          if (currentSubCategory.description != null)
              currentSubCategory.description  = currentSubCategory.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentSubCategory.uid              = attributes.getValue("uid");
          
          if (currentCategory != null) // attribute order independent
          {
                currentSubCategory.parent = currentCategory;
                setUniquePackageAndEmail(currentSubCategory, currentCategory.children);
                currentCategory.children.add(currentSubCategory);
          }
          break;

        case "specific_range":
          printUnsupportedContainedELementMessage("specific_range", attributes.getValue("description"), currentSubCategory);
          break;

        case "specific":
          currentSpecific                  = new SpecificElem();
          currentSpecific.value            = attributes.getValue("value");
          currentSpecific.description      = attributes.getValue("description");
          if (currentSpecific.description != null)
              currentSpecific.description  = currentSpecific.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentSpecific.uid              = attributes.getValue("uid");
          currentSpecific.parent           = currentSubCategory;
          
          if ((currentSubCategory != null) && (currentSubCategory.children != null))
          {
              currentSubCategory.children.add(currentSpecific);
              setUniquePackageAndEmail(currentSpecific, currentSubCategory.children);
          }
          else if (currentExtra != null) // attribute order independent
          {
                currentExtra.parent = currentSpecific;
                setUniquePackageAndEmail(currentExtra, currentSpecific.children);
                currentSpecific.children.add(currentExtra);
          }
          break;

        case "extra":
          currentExtra                  = new ExtraElem();
          currentExtra.value            = attributes.getValue("value");
          currentExtra.description      = attributes.getValue("description");
          if (currentExtra.description != null)
              currentExtra.description  = currentExtra.description.replaceAll("—","-").replaceAll("–","-").replaceAll("\"", "").replaceAll("\'", "");
          currentExtra.uid              = attributes.getValue("uid");
          
          if (currentSpecific != null) // attribute order independent
          {
                currentExtra.parent = currentSpecific;
                setUniquePackageAndEmail(currentExtra, currentSpecific.children);
                currentSpecific.children.add(currentExtra);
          }
          break;

        default:
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
      if (qName == null)
        return;
      if (!qName.equals("cot") && inCot)
        return;

      try {
        switch (qName) {
          case "cot":
            inCot = false;
            break;

          case "cet":
            if (inCetUid30)
              inCetUid30 = false;
            break;

          case "entity":
            currentEntity = null;
            break;

          case "category":
            if (currentCategory != null) //might have been deprecated
                writeCategoryFile(null);
            currentCategory = null; // reset for next time
            break;

          case "subcategory":
            if (currentSubCategory != null) // might have been deprecated
                writeSubCategoryFile(null); // Datapkt
            currentSubCategory = null; // reset for next time
            break;

          case "specific":
            if (currentSpecific != null) // might have been deprecated
              writeSpecificFile(null);
            currentSpecific = null; // reset for next time
            break;

          case "extra":
            if (currentExtra != null) // might have been deprecated
                writeExtraFile(null);
            currentExtra = null; // reset for next time
            break;

          case "subcategory_xref":
          case "subcategory_range":
          case "specific_range":
              // not supported
            break;

          default:
            break;
        }
      }
      catch (Exception ex) {
        System.err.println("endElement qName=" + qName + ", " + ex.getMessage());
        throw new RuntimeException(ex);
      }
    }

    @Override
    public void endDocument() throws SAXException
    {
    }
    
    private void addToPropertiesFile(String pkg, String clsNm, String uid)
    {
      try {
        if(uid2ClassWriter == null)
          buildUid2ClassWriter();
        uid2ClassWriter.write(uid+"="+pkg+"."+clsNm);
        uid2ClassWriter.newLine();
      }
      catch(IOException ex) {
        throw new RuntimeException (ex);
      }
    }
    
    private void buildUid2ClassWriter() throws IOException
    {
      File f = new File(outputDirectory,"uid2EntityClass.properties");
      f.createNewFile();
      uid2ClassWriter = new BufferedWriter(new FileWriter(f));
    }
    
    private void saveEntityFile(DataPkt data, String uid)
    {
        data.sb.append("    }\n}\n");
        saveFile(data.directory, data.clsNm + ".java", data.sb.toString());
        addToPropertiesFile(data.pkg, data.clsNm, uid);
        
        packageInfoPath = data.directory + "/" + "package-info.java";
        File   packageInfoFile = new File(packageInfoPath);
      
        if (!packageInfoFile.exists()) // write package-info.java during first time through
        {
            FileWriter packageInfoFileWriter;
            try {
                packageInfoFile.createNewFile();
                packageInfoFileWriter = new FileWriter(packageInfoFile, StandardCharsets.UTF_8);
                packageInfoBuilder = new StringBuilder();
                packageInfoBuilder.append("/**\n");
                packageInfoBuilder.append(" * Infrastructure classes for ").append(sisoSpecificationTitleDate).append(" enumerations supporting <a href=\"https://github.com/open-dis/open-dis7-java\" target=\"open-dis7-java\">open-dis7-java</a> library.\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append(" * <p> Online: NPS <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500\" target=\"MV3500\">MV3500 Networked Simulation course</a> \n");
            packageInfoBuilder.append(" * links to <a href=\"https://gitlab.nps.edu/Savage/NetworkedGraphicsMV3500/-/tree/master/specifications/README.md\" target=\"README.MV3500\">IEEE and SISO specification references</a> of interest. </p>\n");
            packageInfoBuilder.append(" * <ul> <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=46172\" target=\"SISO-REF-010\" >SISO-REF-010-2020 Reference for Enumerations for Simulation Interoperability</a> </li> \n");
            packageInfoBuilder.append(" *      <li> <a href=\"https://www.sisostds.org/DigitalLibrary.aspx?Command=Core_Download&EntryId=47284\" target=\"SISO-REF-10.1\">SISO-REF-10.1-2019 Reference for Enumerations for Simulation, Operations Manual</a></li> </ul>\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append(" * @see java.lang.Package\n");
                packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful\">https://stackoverflow.com/questions/22095487/why-is-package-info-java-useful</a>\n");
                packageInfoBuilder.append(" * @see <a href=\"https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java\">https://stackoverflow.com/questions/624422/how-do-i-document-packages-in-java</a>\n");
                packageInfoBuilder.append(" */\n");
                packageInfoBuilder.append("\n");
                packageInfoBuilder.append("package ").append(data.pkg).append(";\n");

                packageInfoFileWriter.write(packageInfoBuilder.toString());
                packageInfoFileWriter.flush();
                packageInfoFileWriter.close();
                System.out.println("Created " + packageInfoPath);
            }
            catch (IOException ex) {
                System.out.flush(); // avoid intermingled output
                System.err.println (ex.getMessage()
                   + packageInfoFile.getAbsolutePath()
                );
                ex.printStackTrace(System.err);
            }
        }
    }
  
    private void appendCommonStatements(DataPkt data)
    {
      String contents = String.format(entityCommonTemplate, data.pkg, 
                                      sisoSpecificationTitleDate, data.fullName, 
                                      data.countryNamePretty, data.entKindNmDescription, data.domainValue, data.entityUid,
                                      data.clsNm, data.clsNm, data.countryNm, data.entKindNm, data.domainName, data.domainValue);
      data.sb.append(contents);
    }

    private void appendStatement(DescriptionElem elem, String typ, StringBuilder sb)
    {
      String template = "        set"+typ+"((byte)%s); // uid %s, %s\n";
      if (elem             == null)
          return;
      if (elem.value       == null)
          elem.value        = "";
      if (elem.uid         == null)
          elem.uid          = "";
      if (elem.description == null)
          elem.description  = "";
      sb.append(String.format(template, elem.value, elem.uid, elem.description));
    }
    
    private void writeCategoryFile(DataPkt d)
    {
      DataPkt data = d;
      if (data == null) {
        data = buildEntityCommon(currentCategory.toString(), fixName(currentCategory),currentCategory.uid);
      }
      appendStatement(currentCategory, "Category", data.sb);

      if (d == null) {
        saveEntityFile(data,currentCategory.uid);
      }
    }

    private void writeSubCategoryFile(DataPkt d)
    {
      if ((currentCategory == null) || (currentSubCategory == null))
      {
          System.err.println (this.getClass().getName() + " writeSubCategoryFile has problem with currentCategory or currentSubCategory null, no file written");
          if (d != null)
              System.err.println ("DataPacket d=" + d.sb.toString());
      }
          
      DataPkt data = d;
      if (data == null) {
        data = buildEntityCommon(currentSubCategory.toString(), fixName(currentSubCategory), currentSubCategory.uid);
      }
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);

      if (d == null)
        saveEntityFile(data,currentSubCategory.uid);
    }

    private void writeSpecificFile(DataPkt d)
    {
      DataPkt data = d;
      if (data == null) {
        data = buildEntityCommon(currentSpecific.toString(), fixName(currentSpecific),currentSpecific.uid);
      }
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);
      appendStatement(currentSpecific,    "Specific",    data.sb);

      if (d == null)
        saveEntityFile(data,currentSpecific.uid);
    }

    private void writeExtraFile(DataPkt d)
    {
      DataPkt data = d;
      if (data == null) {
        data = buildEntityCommon(currentExtra.toString(), fixName(currentExtra),currentExtra.uid);
      }
      appendStatement(currentCategory,    "Category",    data.sb);
      appendStatement(currentSubCategory, "SubCategory", data.sb);
      appendStatement(currentSpecific,    "Specific",    data.sb);
      appendStatement(currentExtra,       "Extra",       data.sb);

      if (d == null)
        saveEntityFile(data,currentExtra.uid);
    }

    private DataPkt buildEntityCommon(String fullName, String fixedName, String uid)
    {
        try {
        DataPkt data = new DataPkt();

        if  (fullName == null)
             data.fullName = "";
        else data.fullName = fullName;
        data.sb = new StringBuilder();
//        System.err.println("buildEntityCommon fixedName=" + fixedName + ", uid=" + uid + ", outputDirectory=" + outputDirectory); // debug trace
        
        buildPackagePath(currentEntity, data);
        data.directory = new File(outputDirectory, data.sb.toString());
        data.directory.mkdirs(); // ensure that directory exists

        // Protect against duplicate class names
        int i=1;
        while(new File(data.directory,fixedName+".java").exists()){
          fixedName = fixedName+ i++;
        }
//        System.err.println("fixedName.java=" + fixedName + ".java"); // debug trace

        String packagePath = packageName + "." + pathToPackage(data.sb.toString());
        int    countryInt  = Integer.parseInt(currentEntity.country);
        String countryNm = getName(countryFromInt, countryName, countryInt);

        int domainInt = Integer.parseInt(currentEntity.domain);
        int kindInt   = Integer.parseInt(currentEntity.kind);

        String entityKindName        = getName(kindFromInt, kindName, kindInt);
        String entityKindDescription = legalJavaDoc(getDescription(kindFromInt, kindDescription, kindInt));
        
        String domainName;
        String domainDescription;
        String domainVal;
        switch (entityKindName) {
          case "MUNITION":
            domainName = "MunitionDomain";
            domainDescription = "Munition Domain";
            domainVal = getName(munitionDomainFromInt, munitionDomainName, domainInt);
            break;
          case "SUPPLY":
            domainName = "SupplyDomain";
            domainDescription = "Supply Domain";
            domainVal = getName(supplyDomainFromInt, supplyDomainName, domainInt);
            break;
          case "OTHER":
          case "PLATFORM":
          case "LIFE_FORM":
          case "ENVIRONMENTAL":
          case "CULTURAL_FEATURE":
          case "RADIO":
          case "EXPENDABLE":
          case "SENSOR_EMITTER":
          default:
            domainName = "PlatformDomain";
            domainDescription = "Platform Domain";
            domainVal = getName(methodPlatformDomainFromInt, methodPlatformDomainName, domainInt);
            break;
        }

        data.pkg = packagePath;
        data.entityUid = uid; //currentEntity.uid;
        data.countryNm = countryNm;
        data.entKindNm = entityKindName;
        data.entKindNmDescription = entityKindDescription;
        data.domainName = domainName;
        //data.domainPrettyName = domainDescription;
        data.domainValue = domainVal;
        data.clsNm = fixedName;

        data.sb.setLength(0);

        appendCommonStatements(data);
       return data;
      
      }
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private void saveFile(File parentDir, String name, String contents)
  {
    // save file
    File target = new File(parentDir, name);
    try {
      target.createNewFile();
      try (FileWriter fw = new FileWriter(target, StandardCharsets.UTF_8)) {
        fw.write(contents);
        fw.flush();
      }
    }
    catch (IOException ex) {
      throw new RuntimeException("Error saving " + name + ": " + ex.getLocalizedMessage(), ex);
    }
  }

  private void setUniquePackageAndEmail(DescriptionElem descriptionElement, List<DescriptionElem> descriptionElementList)
  {
      if ((descriptionElement == null) || (descriptionElementList == null))
      {
          if (descriptionElement == null)
              System.err.println(this.getClass().getName() + ".setUniquePackageAndEmail() error: descriptionElement=null");
          if (descriptionElementList == null)
              System.err.println(this.getClass().getName() + ".setUniquePackageAndEmail() error: descriptionElementList=null");
          return;
      }
    String mangledDescription = fixName(descriptionElement);
    mangledDescription = makeUnique(mangledDescription, descriptionElementList);
    descriptionElement.pkgFromDescription  = mangledDescription;
    descriptionElement.enumFromDescription = mangledDescription.toUpperCase();
  }

  private String makeUnique(String s, List<DescriptionElem> lis)
  {
    String news = s;
    for (int i = 1; i < 1000; i++) {
      outer:
      {
        for (DescriptionElem hd : lis) {
          if ((hd.pkgFromDescription != null) && hd.pkgFromDescription.equalsIgnoreCase(news))
            break outer;
        }
        return news;
      }
      news = s + i;
    }
    throw new RuntimeException("Problem generating unique name for " + s);
  }

  Pattern regex = Pattern.compile("\\((.*?)\\)");

  private String buildCountryPackagePart(String s)
  {
    if ((s == null) || s.isEmpty())
        return "";
    Matcher m = regex.matcher(s);
    String fnd = null;
    while (m.find()) {
      fnd = m.group(1);
      if (fnd.length() == 3)
        break;
      fnd = null;
    }
    if (fnd != null)
      return fnd.toLowerCase();

    s = fixName(s);
    s = s.toLowerCase();
    if (s.length() > 3)
      return s.substring(0, 3);
    else
      return s;
  }

/**
 * Naming conventions for cleaning up provided names
 * @param s enumeration string from XML data file
 * @return normalized name
 */
  private String buidKindOrDomainPackagePart(String s)
  {
    s = fixName(s);
    s = s.replaceAll("_", "");
    s = s.toLowerCase();
    return s;
  }
  
  //Country, kind, domain 
  private void buildPackagePath(EntityElem ent, DataPkt data) throws Exception
  {
    if (data == null)
        System.err.println("buildPackagePath data.sb 0: data = null");
    if (data.sb == null)
        System.err.println("buildPackagePath data.sb 0.5: data.sb = null");
//    if  (data.sb.toString().isEmpty())
//         System.err.println("buildPackagePath data.sb 1: empty string");
//    else System.err.println("buildPackagePath data.sb 1: " + data.sb.toString());
    
    // TODO failing here
    String countrydesc = GenerateEntityTypes.this.getDescription(countryFromInt, countryDescription, Integer.parseInt(ent.country));
    if (countrydesc.isEmpty())
    {
        System.err.println(this.getClass().getName() + ".buildPackagePath() failure, no country description");
        return;
    }
//    System.err.println("countrydesc=" + countrydesc);
    data.countryNamePretty = countrydesc;
    data.sb.append(buildCountryPackagePart(countrydesc));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 2: " + data.sb.toString());

    String kindname = getName(kindFromInt, kindName, Integer.parseInt(ent.kind));
    kindname = buidKindOrDomainPackagePart(kindname);
    data.sb.append(buidKindOrDomainPackagePart(kindname));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 3: " + data.sb.toString());

    String domainname;
    String kindnamelc = kindname.toLowerCase();

    switch (kindnamelc) {
      case "munition":
        domainname = getName(munitionDomainFromInt, munitionDomainName, Integer.parseInt(ent.domain));
        break;
      case "supply":
        domainname = getName(supplyDomainFromInt, supplyDomainName, Integer.parseInt(ent.domain));
        break;
      default:
        domainname = getName(methodPlatformDomainFromInt, methodPlatformDomainName, Integer.parseInt(ent.domain));
        break;
    }

    domainname = buidKindOrDomainPackagePart(domainname);
    data.sb.append(buidKindOrDomainPackagePart(domainname));
    data.sb.append("/");
//    System.err.println("buildPackagePathdata.sb 4: " + data.sb.toString());
  }

  private String pathToPackage(String s)
  {
    s = s.replaceAll("_",""); // no underscore divider
    s = s.replace("/", ".");
    if (s.endsWith("."))
        s = s.substring(0, s.length() - 1);
    return s;
  }
/*
  private String parentPackage(String s)
  {
    return s.substring(0, s.lastIndexOf('.'));
  }
*/
  String maybeSpecialCase(String s, String dflt)
  {
    String lc = s.toLowerCase();
    if (lc.equals("united states"))
      return "USA";
    if (lc.equals("not_used"))
      return "";
    return dflt;
  }

  String smallCountryName(String s, String integ)
  {
    if (integ.equals("0"))
      return "";  // "other

    if (s.length() <= 3)
      return s;
    try {
      s = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
      if (s.length() > 3) {
        return maybeSpecialCase(s, integ);
      }
      return s;
    }
    catch (Exception ex) {
      return integ;
    }
  }

  private void printUnsupportedContainedELementMessage(String elname, String eldesc, CategoryElem cat)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(cat.description);
    bldr.append("/");
    bldr.append(cat.parent.kind);
    bldr.append("/");
    bldr.append(cat.parent.domain);
    bldr.append("/");
    bldr.append(cat.parent.country);

    System.err.println("contained XML element " + elname + " (" + eldesc + " in " + bldr.toString() + ") not supported");
  }

  private void printUnsupportedContainedELementMessage(String elname, String eldesc, SubCategoryElem sub)
  {
    StringBuilder bldr = new StringBuilder();
    bldr.append(sub.description);
    bldr.append("/");
    bldr.append(sub.parent.description);
    bldr.append("/");
    bldr.append(sub.parent.parent.kind);
    bldr.append("/");
    bldr.append(sub.parent.parent.domain);
    bldr.append("/");
    bldr.append(sub.parent.parent.country);

    System.err.println("contained XML element " + elname + " (" + eldesc + " in " + bldr.toString() + ") not supported");
  }

  private String legalJavaDoc(String s)
  {
    s = s.replace("<", "&lt;");
    s = s.replace(">", "&gt;");
    s = s.replace("&", "&amp;");
    return s;
  }
  
  private String tryParent(DescriptionElem elem)
  {
    if(elem instanceof ExtraElem )
      return fixName(((ExtraElem)elem).parent.description);

   if(elem instanceof SpecificElem )
      return fixName(((SpecificElem)elem).parent.description);

   if(elem instanceof SubCategoryElem )
      return fixName(((SubCategoryElem)elem).parent.description);

   if(elem instanceof CategoryElem )
      return "uid"+((CategoryElem)elem).parent.uid;

   return null;
  }
  
  private String makeNonNumeric(DescriptionElem elem, String s)
  {
    if(s.startsWith("_"))
      s = s.substring(1);
    
    while(isNumeric(s)) {
      String p = tryParent(elem);
      if(p == null)
        return "_"+s;
      s = p+"_"+s;
    }
    return s;
  }
  
  private boolean isNumeric(String s)
  {
    try {
      int i = Integer.parseInt(s);
      return true;
    }
    catch(NumberFormatException t) {
      return false;
    }
  }
  
  private String fixName(DescriptionElem elem)
  {
    String r = new String();
    if ((elem != null) && (elem.description != null))
    {
        r = fixName(elem.description);
        if(!r.isEmpty() && (isNumeric(r) | isNumeric(r.substring(1))))
        {
          r = makeNonNumeric(elem,r);    
        }
        r = r.substring(0,1) + r.substring(1).replaceAll("_",""); // no underscore divider after first character
    }
    return r;
  }
  
  private String fixName(String s)
  {
    String r = s.trim();
  
    // Convert any of these chars to underbar (u2013 is a hyphen observed in source XML):
    r = r.trim().replaceAll(",", " ").replaceAll("—"," ").replaceAll("-", " ").replaceAll("\\."," ").replaceAll("&"," ")
                                     .replaceAll("/"," ").replaceAll("\"", " ").replaceAll("\'", " ").replaceAll("( )+"," ").replaceAll(" ", "_");
    r = r.substring(0,1) + r.substring(1).replaceAll("_",""); // no underscore divider after first character

    r = r.replaceAll("[\\h-/,\";:\\u2013]", "_");

    // Remove any of these chars (u2019 is an apostrophe observed in source XML):
    r = r.replaceAll("[\\[\\]()}{}'.#&\\u2019]", "");

    // Special case the plus character:
    r = r.replace("+", "PLUS");

    // Collapse all contiguous underbars:
    r = r.replaceAll("_{2,}", "_");

    r = r.replaceAll("<=", "LTE");
    r = r.replaceAll("<",  "LT");
    r = r.replaceAll(">=", "GTE");
    r = r.replaceAll(">",  "GT");
    r = r.replaceAll("=",  "EQ");
    r = r.replaceAll("%",  "pct");
    
    // Java identifier can't start with digit
    if (Character.isDigit(r.charAt(0)))
        r = "_" + r;
    
    if (r.contains("__"))
    {
        System.err.println("fixname contains multiple underscores: " + r);
        r = r.replaceAll("__", "_");
    }
    // If there's nothing there, put in something:
    if (r.trim().isEmpty() || r.equals("_"))
    {
      System.err.print("fixname: erroneous name \"" + s + "\"");
      r = "undefinedName";
      if (!s.equals(r))
           System.err.print( " converted to \"" + r + "\"");
      System.err.println();
    }
    //System.out.println("In: "+s+" out: "+r);
    return r;
  }

  /** GenerateEntityTypes invocation, passing run-time arguments (if any)
     * @param args three configuration arguments, if defaults not used
     */
  public static void main(String[] args)
  {
    try 
    {
        if  (args.length == 0)
             new GenerateEntityTypes("",      "",      ""     ).run(); // use defaults
        else new GenerateEntityTypes(args[0], args[1], args[2]).run();
    }
    catch (SAXException | IOException | ParserConfigurationException ex)
    {
        System.err.println(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
    }
  }
}
