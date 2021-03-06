package com.github.aliabdullaev.dijkstra.linear;

import com.github.aliabdullaev.dijkstra.linear.LinearDijkstraAlgoArray;
import com.github.aliabdullaev.dijkstra.util.performance.PerformanceUtils;
import com.github.aliabdullaev.dijkstra.util.BitArray;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

import static com.github.aliabdullaev.dijkstra.linear.LinearDijkstraAlgoArray.*;


@Data
class SortedHashMapEntry {
    private int cnt = 0;
    private int[] nodeIdVector = new int[10];
    private int bucketId;
    private boolean sorted = false;

    public SortedHashMapEntry(int bucketId) {
        this.bucketId = bucketId;
    }

    /**
     * return pos in bucket of inserted id
     */
    public int insertNodeId(int nodeId, double len) {
        sorted = false;
        if (nodeIdVector.length <= cnt) {
            int newLength = (int) (nodeIdVector.length * 1.5) + 1;
            int[] copy = new int[newLength];
            System.arraycopy(nodeIdVector, 0, copy, 0,
                    nodeIdVector.length);
            nodeIdVector = copy;
        }
        nodeIdVector[cnt] = nodeId;
        return cnt++;
    }

    /**
     * return id of moved element in bucket
     */
    public int eraseNodeId(int pos) {
        sorted = false;
        cnt--;
        nodeIdVector[pos] = nodeIdVector[cnt];
        nodeIdVector[cnt] = 0;
        if (pos == cnt) {
            return -1;
        }
        return nodeIdVector[pos];
    }

    public int getLastId() {
        return nodeIdVector[cnt - 1];
    }
}

@Slf4j
@Data
public class SortedHashMap {
    public static int initSize = 4000;
    private SortedHashMapEntry[] arr;
    private LinearDijkstraAlgoArray dataArr;
    private int minEl = 0;
    private int maxEl = 0;
    private int maxId = 400;
    private int _size = 0;
    private BitArray idMap;
    public static final int BUCKET_SIZE = 20;

    public SortedHashMap(int firstId, int firstEl, LinearDijkstraAlgoArray dijkstraSearchArrays) {
        this(dijkstraSearchArrays);
        init(firstId, firstEl);
    }

    private void initArray() {
        arr = new SortedHashMapEntry[initSize];
        for (int i=0; i< initSize; i++)
            arr[i] = new SortedHashMapEntry(i);
    }

    public SortedHashMap(LinearDijkstraAlgoArray dijkstraSearchArrays) {
        initArray();
        this.dataArr = dijkstraSearchArrays;
        this.idMap = new BitArray(dijkstraSearchArrays.size());
    }

    public void init(int firstId, int firstEl) {
        minEl = firstEl/ BUCKET_SIZE;
        maxEl = firstEl/ BUCKET_SIZE;
        resize();
        insert(firstId, firstEl);
    }

    // update - id узла, новая длина до узла в секундах
    public final void insert(int id, double newLen) {
        int bucketNum = (int) (newLen / BUCKET_SIZE);
        minEl = PerformanceUtils.getMin(minEl, bucketNum);
        maxEl = PerformanceUtils.getMax(maxEl, bucketNum);
        _size++;
        resize();
        int posInBucket = arr[bucketNum].insertNodeId(id, newLen);
        dataArr.setBucketNum(id, bucketNum);
        dataArr.setPosInBucket(id, posInBucket);
        idMap.set(id);
    }


    private final void rebalance() {
        int oldMinEl = minEl;
        for (; minEl<=maxEl && arr[minEl].getCnt() == 0; minEl++);
        freeArraysFromOldAndNewMinEl(oldMinEl, minEl);
        for (; maxEl>minEl && arr[maxEl].getCnt() == 0; maxEl--);
    }

    public int getMinId() {
        return arr[minEl].getLastId();
    }

    public final void erase(int id) {
        if (idMap.get(id) == 0) {
            return;
        }
        int bucketNum = dataArr.getBucketNum(id);
        int movedNodeId = arr[bucketNum].eraseNodeId(dataArr.getPosInBucket(id));
        if (movedNodeId != -1) {
            dataArr.setPosInBucket(movedNodeId, dataArr.getPosInBucket(id));
        }

        // итерация пока не найдем позицию нового минимального элемента
        rebalance();

        idMap.reset(id);
        _size--;
        if (_size == 0) {
            minEl = 1_000_000;
            maxEl = -1;
        }
    }


    private void resize() {
        int len = arr.length;
        if (len < maxEl + 1000) {
            arr = Arrays.copyOf(arr, maxEl + 1000);
            for (int i = len; i < arr.length; i++)
                arr[i] = new SortedHashMapEntry(i);
        }
    }

    public final int size() { return _size; }

    private void freeArraysFromOldAndNewMinEl(int oldMinEl, int newMinEl) {
        for (int i=oldMinEl; i < newMinEl - 2; i++) {
            arr[i] = null; // освобождаем память
        }
    }
}
