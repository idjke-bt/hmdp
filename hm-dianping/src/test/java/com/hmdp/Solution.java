package com.hmdp;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Solution {

    public boolean exist(char[][] board, String word) {
        int[][] visited = new int[board.length][board[0].length];
        for (int i=0; i< board.length; i++){
            for(int j=0; j<board[0].length; j++){
                Arrays.stream(visited).forEach(x -> Arrays.fill(x,0));
                if(back(i,j,board,word,0,visited))
                    return true;
            }
        }
        return false;
    }

    public boolean back(int x, int y, char[][] board, String word, int k, int[][] visited){
        if (k==word.length())
            return true;
        if(x<0 || y<0 || x>= board.length || y>=board[0].length)
            return false;
        if(visited[x][y]==-1)
            return false;
        if(board[x][y]!=word.charAt(k))
            return false;
        visited[x][y] = -1;
        boolean flag =  back(x+1,y,board,word,k+1,visited) || back(x-1,y,board,word,k+1,visited)
                ||back(x,y-1,board,word,k+1,visited) ||back(x,y+1,board,word,k+1,visited);
        if(!flag)
            visited[x][y] = 0;
        return flag;
    }



    public static void main(String[] args) {


    }
}
