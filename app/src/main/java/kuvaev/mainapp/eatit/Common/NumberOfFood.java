package kuvaev.mainapp.eatit.Common;

public class NumberOfFood {
    public static String convertIdToName(String code){
        switch (code) {
            case "1":
                return "1-Pc Cheesy Chicken Rice";
            case "2":
                return "1-Pc Chicken Rice";
            case "3":
                return "2-Pc Cheesy Chicken Rice";
            case "4":
                return "2-Pc Chicken Rice";
            case "5":
                return "1-Pc Chicken Porridge";
            case "6":
                return "2-Pc Meal";
            case "7":
                return "3-Pc Meal";
            case "8":
                return "Half-Spring Chicken Meal";
            case "9":
                return "6-Pcs Chicken Nuggets Meal";
            case "10":
                return "Single Cheesy Burger Set Meal";
            case "11":
                return "Single Premium Burger Set Meal";
            case "12":
                return "Double Cheesy Burger Set Meal";
            case "13":
                return "Double Premium Burger Set Meal";
            case "14":
                return "Full-Spring Chicken Meal";
            case "15":
                return "2 Person Combo";
            case "16":
                return "3 Person Combo";
            case "17":
                return "5 Person Combo";
            case "18":
                return "Potato Platter (L)";
            case "19":
                return "Criss Cut Fries (L)";
            case "20":
                return "Borenos Rice";
            case "21":
                return "Cheesy Wedges (L)";
            case "22":
                return "Chicken Porridge";
            case "23":
                return "Mashed Potato (L)";
            case "24":
                return "3 Pc Fried Buns";
            case "25":
                return "Coleslaw (L)";
            case "26":
                return "Chicken Nuggets (6-Pcs)";
            case "27":
                return "Chicken Nuggets (10-Pcs)";
            case "28":
                return "Chicken Nuggets (21-Pcs)";
            case "29":
                return "Borenos Sauce";
            case "30":
                return "Cheese Dip";
            case "31":
                return "Tartar Sauce";
            case "32":
                return "Onion Sour Cream Sauce";
            case "33":
                return "1-Pc Chicken";
            case "34":
                return "2-Pc Chicken";
            case "35":
                return "3-Pc Chicken";
            case "36":
                return "5-Pc Chicken";
            case "37":
                return "9-Pc Chicken";
            case "38":
                return "15-Pc Chicken";
            case "39":
                return "Full Spring A La Carte";
            case "40":
                return "Coca Cola Tin";
            case "41":
                return "Ice Lemon Tea Tin";
            case "42":
                return "Passionfruit Tea Tin";
            case "43":
                return "Ayataka Green Tea Tin";
            case "44":
                return "Sprite Bottle (1.5L)";
            case "45":
                return "Coca Cola Bottle (1.5L)";
            case "46":
                return "AL 5 (Promo) - 5 Pc Chicken";
            default:
                return null;
        }
    }
}
