package it.uniroma3.siwprogetto.model;

        import jakarta.persistence.*;
        import java.util.List;

        @Entity
        public class Dealer {
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            private String name;
            private String description;
            private String address;
            private String phone;
            private String email;

            @OneToOne
            @JoinColumn(name = "owner_id")
            private User owner;

            @OneToMany(mappedBy = "dealer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
            private List<Image> images;

            // Getters e Setters
            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
            public String getAddress() { return address; }
            public void setAddress(String address) { this.address = address; }
            public String getPhone() { return phone; }
            public void setPhone(String phone) { this.phone = phone; }
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
            public User getOwner() { return owner; }
            public void setOwner(User owner) { this.owner = owner; }
            public List<Image> getImages() { return images; }
            public void setImages(List<Image> images) { this.images = images; }
        }