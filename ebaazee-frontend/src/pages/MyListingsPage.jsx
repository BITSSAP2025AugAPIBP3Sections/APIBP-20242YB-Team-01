import React, { useEffect, useState } from 'react';
import axios from 'axios';
import styles from '../css/MyListingsPage.module.css';

export default function MyListingsPage() {
    const [listings, setListings] = useState([]);
    const [filteredListings, setFilteredListings] = useState([]);
    const [categories, setCategories] = useState([]);
    const [selectedCategory, setSelectedCategory] = useState(null);
    const [selectedStatus, setSelectedStatus] = useState(null);

    // Fetch listings
    useEffect(() => {
        const fetchListings = async () => {
            try {
                const token = localStorage.getItem("token");
                const userId = localStorage.getItem("id"); // Get seller's user ID from localStorage
                
                if (!userId) {
                    console.error("User ID not found in localStorage");
                    return;
                }

                const response = await axios.get(`http://localhost:8080/api/products/v1/users/${userId}`, {
                    headers: {
                        Authorization: `Bearer ${token}`
                    }
                });

                setListings(response.data);
                setFilteredListings(response.data);
            } catch (error) {
                console.error("Error fetching listings:", error);
            }
        };

        fetchListings();
    }, []);

    // Fetch categories
    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const token = localStorage.getItem("token");
                const response = await axios.get("http://localhost:8080/api/categories/v1", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                
                // Extract category names from the response
                const categoryNames = response.data.map(cat => cat.name || cat);
                setCategories(categoryNames);
            } catch (error) {
                console.error("Error fetching categories:", error);
            }
        };

        fetchCategories();
    }, []);

    // Apply filters
    useEffect(() => {
        let filtered = [...listings];

        if (selectedCategory) {
            filtered = filtered.filter(item => item.category === selectedCategory);
        }
        if (selectedStatus) {
            filtered = filtered.filter(item => item.status === selectedStatus);
        }

        setFilteredListings(filtered);
    }, [selectedCategory, selectedStatus, listings]);

    // Reset filters handler
    const resetFilters = () => {
        setSelectedCategory(null);
        setSelectedStatus(null);
    };

    return (
        <div className={styles.pageWrapper}>
            <h1 className={styles.pageTitle}>My Listings</h1>

            <div className={styles.filtersContainer}>
                <select
                    className={styles.filterSelect}
                    value={selectedCategory || ''}
                    onChange={(e) => setSelectedCategory(e.target.value || null)}
                >
                    <option value="">All Categories</option>
                    {categories.map((cat, index) => (
                        <option key={`${cat}-${index}`} value={cat}>{cat}</option>
                    ))}
                </select>

                <select
                    className={styles.filterSelect}
                    value={selectedStatus || ''}
                    onChange={(e) => setSelectedStatus(e.target.value || null)}
                >
                    <option value="">All Statuses</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="FROZEN">FROZEN</option>
                    <option value="SOLD">SOLD</option>
                </select>

                <button
                    className={styles.resetButton}
                    onClick={resetFilters}
                    type="button"
                >
                    Reset Filters
                </button>
            </div>

            <div className={styles.cardContainer}>
                {filteredListings.length === 0 ? (
                    <div className={styles.emptyState}>
                        <p>No listings found. Create your first listing!</p>
                    </div>
                ) : (
                    filteredListings.map((listing) => {
                        // Determine status based on frozen and sold flags
                        let status = 'ACTIVE';
                        if (listing.sold) {
                            status = 'SOLD';
                        } else if (listing.frozen) {
                            status = 'FROZEN';
                        }

                        return (
                            <div key={listing.id} className={styles.card}>
                                <h2 className={styles.productTitle}>{listing.name}</h2>
                                <p className={styles.description}>{listing.description || 'No description available'}</p>
                                <p className={styles.status}>
                                    Status: <strong>{status}</strong>
                                </p>
                                <p className={styles.category}>
                                    Category: <strong>{listing.category}</strong>
                                </p>
                                <p className={styles.bidRange}>
                                    Bid Range: <strong>${listing.minBid} - ${listing.maxBid}</strong>
                                </p>
                                {listing.currentBid > 0 && (
                                    <p className={styles.currentBid}>
                                        Current Bid: <strong>${listing.currentBid}</strong>
                                    </p>
                                )}
                                {listing.endTime && (
                                    <p className={styles.endTime}>
                                        End Time: <strong>{new Date(listing.endTime).toLocaleString()}</strong>
                                    </p>
                                )}
                                {listing.sold && listing.buyerId && (
                                    <p className={styles.buyerInfo}>
                                        <strong>Sold to Buyer ID:</strong> {listing.buyerId}
                                    </p>
                                )}
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}
