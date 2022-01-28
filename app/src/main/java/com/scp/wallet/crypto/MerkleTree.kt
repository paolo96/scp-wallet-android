package com.scp.wallet.crypto

import com.scp.wallet.crypto.Crypto.Companion.crypto

class MerkleTree {

    companion object {

        private val LEAF_PREFIX = byteArrayOf(0x00)
        private val NODE_PREFIX = byteArrayOf(0x01)

    }

    private var stack = mutableListOf<Pair<Int, ByteArray>>()

    private var proofSet = arrayListOf<ByteArray>()
    private var proofIndex = 0
    private var currentIndex = 0
    private var proofBase = byteArrayOf()

    fun root() : ByteArray {

        if(stack.size == 0) {
            return ByteArray(32) { 0 }
        }

        var current = stack[stack.size - 1]
        for(i in stack.size - 2 downTo 0) {
            current = joinSubTrees(stack[i], current)
        }
        return current.second

    }

    fun push(data: ByteArray) {

        if(currentIndex == proofIndex) {
            proofBase = data
            proofSet.add(leafSum(data))
        }

        stack.add(Pair(0, leafSum(data)))
        joinAllSubTrees()
        currentIndex++

    }

    private fun leafSum(data: ByteArray) : ByteArray {
        return crypto.blake2b(LEAF_PREFIX + data)
    }

    private fun nodeSum(a: ByteArray, b: ByteArray) : ByteArray {
        return crypto.blake2b(NODE_PREFIX + a + b)
    }

    private fun joinSubTrees(a: Pair<Int, ByteArray>, b: Pair<Int, ByteArray>) : Pair<Int, ByteArray> {
        if(a.first < b.first) {
            println("[Warning] INVALID SUBTREE PRESENTED HEIGHT MISMATCH")
        }
        val nodeSum = nodeSum(a.second, b.second)

        return Pair(a.first+1, nodeSum)
    }

    private fun joinAllSubTrees() {

        while(stack.size > 1 && stack[stack.size - 1].first == stack[stack.size - 2].first) {
            val i = stack.size - 1
            val j = stack.size - 2

            if(stack[i].first == proofSet.size-1) {
                val leaves = 1 shl stack.size
                val mid = (currentIndex / leaves) * leaves
                proofSet += if(proofIndex < mid) {
                    stack[i].second
                } else {
                    stack[j].second
                }

                if(proofIndex < mid-leaves) {
                    println("[Warning] PROOF WITH WEIRD LEAVES")
                }
            }

            val stackNew = stack.subList(0, j)
            stackNew.add(joinSubTrees(stack[j], stack[i]))
            stack = stackNew
        }

    }

}