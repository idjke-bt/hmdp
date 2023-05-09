package com.hmdp;

import java.util.*;

public class Solution {
    public int[][] merge(int[][] intervals) {
        List<int[]> list = new ArrayList<>();
        Arrays.sort(intervals, Comparator.comparingInt(arr -> arr[0]));
        int l=intervals[0][0], r=intervals[0][1];
        for (int[] interval : intervals) {
            if (interval[0] > r) {
                list.add(new int[]{l, r});
                l = interval[0];
            }
            r = Math.max(r, interval[1]);
        }
        list.add(new int[]{l, r});
        return list.toArray(new int[0][]);
    }


    public static void main(String[] args) {

    }
}
