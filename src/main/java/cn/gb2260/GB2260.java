package cn.gb2260;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class GB2260 {
    private final Revision revision;
    private HashMap<String, String> data;
    private ArrayList<Division> provinces;

    public GB2260() {
        this(Revision.V2018);
    }

    public GB2260(Revision revision) {
        this.revision = revision;
        data = new HashMap<String, String>();
        provinces = new ArrayList<Division>();
        InputStream inputStream = getClass().getResourceAsStream("/data/" + revision.getCode() + ".txt");
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (r.ready()) {
                String line = r.readLine();
                String[] split = line.split("\t");
                String code = split[0];
                String name = split[1];

                data.put(code, name);

                if (Pattern.matches("^\\d{2}0{4}$", code)) {
                    Division division = new Division();
                    division.setCode(code);
                    division.setName(name);
                    provinces.add(division);
                }
            }
        } catch (IOException e) {
            System.err.println("Error in loading GB2260 data!");
            throw new RuntimeException(e);
        }
    }

    public Division getDivision(String code) {
        if (code.length() != 6) {
            throw new InvalidCodeException("Invalid code");
        }

        if (!data.containsKey(code)) {
            return null;
        }

        Division division = new Division();
        division.setName(data.get(code));
        division.setRevision(getRevision().getCode());
        division.setCode(code);

        if (Pattern.matches("^\\d{2}0{4}$", code)) {
            return division;
        }

        String provinceCode = code.substring(0, 2) + "0000";
        division.setProvince(data.get(provinceCode));

        if (Pattern.matches("^\\d{4}0{2}$", code)) {
            return division;
        }

        String prefectureCode = code.substring(0, 4) + "00";
        division.setPrefecture(data.get(prefectureCode));

        division.setRevision(this.revision.getCode());
        return division;
    }

    public Revision getRevision() {
        return revision;
    }

    public ArrayList<Division> getProvinces() {
        return provinces;
    }

    /**
     * 通过省code获取市列表
     * @param code
     * @return
     */
    public ArrayList<Division> getPrefectures(String code)  {
        ArrayList<Division> rv = new ArrayList<Division>();

        if (!Pattern.matches("^\\d{2}0{4}$", code)) {
            throw new InvalidCodeException("Invalid province code");
        }

        if (!data.containsKey(code)) {
            throw new InvalidCodeException("Province code not found");
        }

        Division province = getDivision(code);

        Pattern pattern = Pattern.compile("^" + code.substring(0, 2) + "\\d{2}00$");
        for (String key : data.keySet()) {
            if (pattern.matcher(key).matches()) {
                Division division = getDivision(key);
                division.setProvince(province.getName());
                rv.add(division);
            }
        }

        return rv;
    }

    /**
     * 通过市code获取县区列表
     * @param code
     * @return
     */
    public ArrayList<Division> getCounties(String code)  {
        ArrayList<Division> rv = new ArrayList<Division>();

        if (!Pattern.matches("^\\d+[1-9]0{2,3}$", code)) {
            throw new InvalidCodeException("Invalid prefecture code");
        }

        if (!data.containsKey(code)) {
            throw new InvalidCodeException("Prefecture code not found");
        }

        Division prefecture = getDivision(code);
        Division province = getDivision(code.substring(0, 2) + "0000");

        Pattern pattern = Pattern.compile("^" + code.substring(0, 4) + "\\d+$");
        for (String key : data.keySet()) {
            if (pattern.matcher(key).matches()) {
                Division division = getDivision(key);
                division.setProvince(province.getName());
                division.setPrefecture(prefecture.getName());
                rv.add(division);
            }
        }

        return rv;
    }

    public static void printSql(String code,String name,String fullName,String parentCode ){
        parentCode = parentCode == null?"100000":parentCode;
        System.out.println("insert into tb_sys_area VALUES('"+code+"','"+name+"','"+parentCode+"',\t'"+fullName+ "','201809');");
    }

    public static void main(String[] args) {
        System.out.println();
        GB2260 g = new GB2260();
        for(Division division : g.provinces){
//            System.out.println(division.getCode()+"\t"+division.getName()+"\t"+100000+"\t"+division.getName());
            printSql(division.getCode(),division.getName(),division.getName(),null);
            ArrayList<Division> cityList =  g.getPrefectures(division.getCode());
            for(Division city : cityList){
                if(!city.getName().equalsIgnoreCase(city.getProvince())){
//                    System.out.println(city.getCode()+"\t"+city.getName()+"\t"+division.getCode()+"\t"+division.getName()+" "+city.getName());
                    printSql(city.getCode(),city.getName(),division.getName()+" "+city.getName(),division.getCode());
                    ArrayList<Division> countyList =  g.getCounties(city.getCode());
                    for(Division county : countyList){
                        if(!county.getName().equalsIgnoreCase(county.getPrefecture())){
//                            System.out.println(county.getCode()+"\t"+county.getName()+"\t"+city.getCode()+"\t"+division.getName()+" "+city.getName()+" "+county.getName());
                            printSql(county.getCode(),county.getName(),division.getName()+" "+city.getName()+" "+county.getName(),city.getCode());
                        }
                    }
                }

            }
        }

    }
}
