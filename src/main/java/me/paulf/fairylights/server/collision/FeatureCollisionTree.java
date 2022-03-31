package me.paulf.fairylights.server.collision;

import me.paulf.fairylights.server.feature.Feature;
import me.paulf.fairylights.server.feature.FeatureType;
import me.paulf.fairylights.util.Mth;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class FeatureCollisionTree implements Collidable {
    private final FeatureType type;

    public final AxisAlignedBB[] tree;

    private final Feature[] nodeToFeature;

    private FeatureCollisionTree(final FeatureType type, final AxisAlignedBB[] tree, final Feature[] nodeToFeature) {
        this.type = type;
        this.tree = tree;
        this.nodeToFeature = nodeToFeature;
    }

    @Nullable
    @Override
    public Intersection intersect(final Vector3d origin, final Vector3d end) {
        return this.intersect(origin, end, 0);
    }

    @Nullable
    private Intersection intersect(final Vector3d origin, final Vector3d end, final int node) {
        final Vector3d result;
        if (this.tree[node].func_72318_a(origin)) {
            result = origin;
        } else {
            result = this.tree[node].func_216365_b(origin, end).orElse(null);
        }
        // If there is no intersection then there is no child intersection
        if (result == null) {
            return null;
        }
        // Check if leaf
        final int nL = node * 2 + 1;
        if (nL >= this.tree.length || this.tree[nL] == null) {
            return new Intersection(result, this.tree[node], this.type, this.nodeToFeature[node]);
        }
        // Intersect left
        final Intersection intersection = this.intersect(origin, end, nL);
        if (intersection != null) {
            return intersection;
        }
        // Intersect right
        return this.intersect(origin, end, node * 2 + 2);
    }


    public static <T extends Feature> FeatureCollisionTree build(final FeatureType type, final T[] features, final Function<T, AxisAlignedBB> mapper) {
        return build(type, features, mapper, 0, features.length - 1);
    }

    public static <T extends Feature> FeatureCollisionTree build(final FeatureType type, final T[] features, final Function<T, AxisAlignedBB> mapper, final int start, final int end) {
        return build(type, i -> features[i], i -> mapper.apply(features[i]), start, end);
    }

    public static <T extends Feature> FeatureCollisionTree build(final FeatureType type, final IntFunction<T> features, final IntFunction<AxisAlignedBB> mapper, final int start, final int end) {
        final AxisAlignedBB[] tree = new AxisAlignedBB[end == 0 ? 1 : (1 << (Mth.log2(end - start) + 2)) - 1];
        final Feature[] treeFeatures = new Feature[tree.length];
        tree[0] = build(features, mapper, tree, treeFeatures, start, end, 0);
        return new FeatureCollisionTree(type, tree, treeFeatures);
    }

    private static <T extends Feature> AxisAlignedBB build(final IntFunction<T> features, final IntFunction<AxisAlignedBB> mapper, final AxisAlignedBB[] tree, final Feature[] treeFeatures, final int min, final int max, final int node) {
        if (min > max) {
            throw new IllegalStateException(String.format("min > max, tree: %s, min: %d, max: %d, node: %d", Arrays.toString(tree), min, max, node));
        }
        if (min == max) {
            treeFeatures[node] = features.apply(min);
            return mapper.apply(min);
        }
        final int mid = min + (max - min) / 2;
        final int nL = node * 2 + 1;
        final int nR = node * 2 + 2;
        return (tree[nL] = build(features, mapper, tree, treeFeatures, min, mid, nL)).func_111270_a(tree[nR] = build(features, mapper, tree, treeFeatures, mid + 1, max, nR));
    }
}
