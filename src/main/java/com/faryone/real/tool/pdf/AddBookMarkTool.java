package com.faryone.real.tool.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.*;
import java.util.Stack;

/**
 * @author faryone
 */
public class AddBookMarkTool {
    
    public static void main(String[] args) throws Exception {
        String path = "/";

        String sourceFilePath = "rfc3550.pdf";
        String bookMarkFile = "mark.txt";
        
        
        sourceFilePath = path + sourceFilePath;
        String desFilePath = sourceFilePath + ".pdf";
        bookMarkFile = path + bookMarkFile;

        int startPageOffset = 0;
    

        PDDocument doc = PDDocument.load(new File(sourceFilePath));
    
        BufferedReader reader = new BufferedReader(new FileReader(bookMarkFile));
        PDDocumentOutline outline = new PDDocumentOutline();
        Stack<PDOutlineItem> itemStack = new Stack<>();
    
        String line = reader.readLine();
        while (line != null){
            try {
                parseBookMarkFile(itemStack, doc, outline, startPageOffset, line);
            } catch (Exception throwable){
                System.out.println(line);
                throw throwable;
            }
        
            line = reader.readLine();
        }

        doc.getDocumentCatalog().setDocumentOutline(outline);
        doc.save(new File(desFilePath));


        System.out.println("add mark done......");
    }

    
    private static void parseBookMarkFile(Stack<PDOutlineItem> itemStack, PDDocument doc,
        PDDocumentOutline outline, int startPageOffset, String originLine){
        
        // 删除两端空格
        String line = originLine.trim();
    
    
        /**
         * 将整行切成三部分
         *
         * 第一部分: 书签的层级位置
         * 第二部分: 书签的标题
         * 第三部分: 书签跳转的目标页码(实际的页位置需要加上startPageOffset)
         */
        int firstPartEnd = line.indexOf(" ");
        if (firstPartEnd <= 0){
            System.out.println("书签解析有问题，每行信息不是由三部分组成。出问题的行: " + originLine);
            throw new RuntimeException();
        }
    
    
        int secondPartEnd = -1;
        
        for (int i=line.length()-1;i>=0;i--){
            if (isDigital(line.charAt(i))){
                continue;
            }
            secondPartEnd = i;
            break;
        }

        if (secondPartEnd <= 0){
            System.out.println("书签解析有问题，每行信息不是由三部分组成。出问题的行: " + originLine);
            throw new RuntimeException();
        }

        String hierarchy = line.substring(0, firstPartEnd);
        if (hierarchy.charAt(hierarchy.length()-1) == '.'){
            hierarchy = hierarchy.substring(0, hierarchy.length()-1);
        }
        hierarchy = hierarchy.trim();
    
        String title = line.substring(firstPartEnd+1, secondPartEnd);
        title = title.trim();
        // 删除无用的 .......
        title = title.replaceAll("\\.", "");
    
        String page = line.substring(secondPartEnd+1);
        page = page.trim();
    
    
        addMark(itemStack, doc, outline, hierarchy, title, page, startPageOffset, originLine);
        System.out.println(hierarchy + " " + title + " " +page);
    }
    
    
    
    private static void addMark(Stack<PDOutlineItem> itemStack, PDDocument doc, PDDocumentOutline outline,
        String hierarchy, String tile, String page, int startPageOffset, String line){

        // 特殊处理目录
        if(tile.equals("目录")){
            PDPage pdPage = doc.getPage(Integer.parseInt(page) - 1);
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(tile);
            item.setDestination(pdPage);
            outline.addFirst(item);
            return;
        }

        int location = Integer.parseInt(page) + startPageOffset - 1;
        PDPage pdPage = doc.getPage(location);
        
        PDOutlineItem item = new PDOutlineItem();
        item.setTitle(hierarchy + " " +tile);
        item.setDestination(pdPage);
        
        
        String[] hierarchies =  hierarchy.split("\\.");
        
        
        // 是第一级目录
        if (hierarchies.length == 1){
            itemStack.clear();
            itemStack.add(item);

            outline.addLast(item);
            return;
        }
        
        
        int itemSizeNotNeed = itemStack.size() - hierarchies.length + 1;
        // 如果当前目录比上一个item的目录长度相同或更短
        if (itemSizeNotNeed > 0){
            for (int i=0;i<itemSizeNotNeed;i++){
                itemStack.remove(itemStack.size()-1);
            }
        }
        
    
        PDOutlineItem parentItem = itemStack.get(itemStack.size() - 1);
        
        parentItem.addLast(item);
        
        itemStack.add(item);
    }
    
    
    /**
     * 是否是数字
     */
    private static boolean isDigital(char c){
        if (c >= '0' && c <= '9'){
            return true;
        } else {
            return false;
        }
    }
}
