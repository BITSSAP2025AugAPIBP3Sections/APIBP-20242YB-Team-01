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
        axios.get('http://localhost:9090/api/seller/my-listings', {
            headers: {
                Authorization: `Bearer ${localStorage.getItem("token")}`
            }
        })
            .then(response => {
                setListings(response.data);
                setFilteredListings(response.data);
            })
            .catch(error => console.error("Error fetching listings:", error));
    }, []);

    // Fetch categories
    useEffect(() => {
        const fetchCategories = async () => {
            try {
                const token = localStorage.getItem("token");
                const response = await axios.get("http://localhost:9090/api/categories", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                setCategories(response.data);
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
                {filteredListings.map((listing) => (
                    <div key={listing.id} className={styles.card}>
                        <h2 className={styles.productTitle}>{listing.name}</h2>
                        <p className={styles.description}>{listing.description}</p>
                        <p className={styles.status}>
                            Status: <strong>{listing.status}</strong>
                        </p>
                        <p className={styles.category}>
                            Category: <strong>{listing.category}</strong>
                        </p>
                        {listing.status === "SOLD" && (
                            <>
                                <p><strong>Sold To:</strong> {listing.buyer}</p>
                                <p><strong>Sold Price:</strong> ₹{listing.price}</p>
                            </>
                        )}
                        {listing.status === "ACTIVE" && listing.currentBid && (
                            <>
                                <p><strong>Highest Bid:</strong> ₹{listing.currentBid}</p>
                                <p><strong>Top Bidder:</strong> {listing.topBidder}</p>
                            </>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}
