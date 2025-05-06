    package it.uniroma3.siwprogetto.model;

    import jakarta.persistence.*;
    import com.fasterxml.jackson.annotation.JsonManagedReference;


    @Entity
    public class Dealer {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        private String description;
        private String address;
        private String contact;
        private Double lat;
        private Double lng;
        private String imagePath;

        @OneToOne
        @JoinColumn(name = "owner_id")
        private User owner;

        public Dealer() {
            this.lat = 0.0;
            this.lng = 0.0;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        public User getOwner() { return owner; }
        public void setOwner(User owner) { this.owner = owner; }


    }